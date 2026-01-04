package com.farukg.movievault.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.farukg.movievault.core.error.AppErrorException
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.cache.CachePolicy
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.CatalogRemoteKeysDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import com.farukg.movievault.data.local.entity.CatalogRemoteKeyEntity
import com.farukg.movievault.data.local.entity.MovieEntity
import com.farukg.movievault.data.local.model.CatalogMovieRow
import com.farukg.movievault.data.remote.CatalogRemoteDataSource

@OptIn(ExperimentalPagingApi::class)
class CatalogRemoteMediator(
    private val db: MovieVaultDatabase,
    private val movieDao: MovieDao,
    private val remoteKeysDao: CatalogRemoteKeysDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val remote: CatalogRemoteDataSource,
    private val clock: Clock,
) : RemoteMediator<Int, CatalogMovieRow>() {

    override suspend fun initialize(): InitializeAction {
        val now = clock.now()
        val lastUpdated =
            cacheMetadataDao.get(CacheKeys.CATALOG_LAST_UPDATED)?.lastUpdatedEpochMillis
        val hasCatalog = movieDao.countCatalogMovies() > 0

        return if (!hasCatalog || CachePolicy.isCatalogStale(lastUpdated, now)) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CatalogMovieRow>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> 1

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val last =
                        state.lastItemOrNull()
                            ?: return MediatorResult.Success(endOfPaginationReached = false)

                    val key =
                        remoteKeysDao.remoteKeyByMovieId(last.id)
                            ?: return MediatorResult.Success(endOfPaginationReached = false)

                    key.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

        return when (val result = remote.fetchPopularPage(page)) {
            is AppResult.Error -> {
                MediatorResult.Error(AppErrorException(result.error))
            }

            is AppResult.Success -> {
                val pageData = result.data
                val incoming = pageData.results
                val endOfPaginationReached =
                    incoming.isEmpty() || pageData.page >= pageData.totalPages

                val distinctIncoming = incoming.distinctBy { it.id }
                val ids = distinctIncoming.map { it.id }
                val existingById = movieDao.getMoviesByIds(ids).associateBy { it.id }

                val baseRank = (page - 1) * state.config.pageSize

                db.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        remoteKeysDao.clearRemoteKeys()
                        movieDao.clearCatalogRanks()
                    }

                    val entities =
                        distinctIncoming.mapIndexed { index, movie ->
                            val existing = existingById[movie.id]
                            MovieEntity(
                                id = movie.id,
                                title = movie.title,
                                releaseYear = movie.releaseYear,
                                posterUrl = movie.posterUrl,
                                rating = movie.rating,
                                overview = existing?.overview,
                                runtimeMinutes = existing?.runtimeMinutes,
                                genres = existing?.genres ?: emptyList(),
                                popularRank = baseRank + index,
                                detailFetchedAtEpochMillis = existing?.detailFetchedAtEpochMillis,
                            )
                        }

                    movieDao.upsertAll(entities)

                    val keys =
                        distinctIncoming.map { movie ->
                            CatalogRemoteKeyEntity(
                                movieId = movie.id,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (endOfPaginationReached) null else page + 1,
                            )
                        }
                    remoteKeysDao.insertAll(keys)

                    if (loadType == LoadType.REFRESH) {
                        cacheMetadataDao.upsert(
                            CacheMetadataEntity(
                                key = CacheKeys.CATALOG_LAST_UPDATED,
                                lastUpdatedEpochMillis = clock.now(),
                            )
                        )
                    }
                }

                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
        }
    }
}
