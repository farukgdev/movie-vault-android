package com.farukg.movievault.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.farukg.movievault.core.error.AppErrorException
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.cache.CachePolicy
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.CatalogRemoteKeysDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import com.farukg.movievault.data.local.entity.CatalogRemoteKeyEntity
import com.farukg.movievault.data.local.entity.MovieEntity
import com.farukg.movievault.data.local.model.MovieWithFavorite
import com.farukg.movievault.data.remote.CatalogRemoteDataSource

@OptIn(ExperimentalPagingApi::class)
class CatalogRemoteMediator(
    private val db: MovieVaultDatabase,
    private val movieDao: MovieDao,
    private val remoteKeysDao: CatalogRemoteKeysDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val remote: CatalogRemoteDataSource,
) : RemoteMediator<Int, MovieWithFavorite>() {

    override suspend fun initialize(): InitializeAction {
        val now = System.currentTimeMillis()
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
        state: PagingState<Int, MovieWithFavorite>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> {
                    val anchor = state.anchorPosition?.let { state.closestItemToPosition(it) }
                    val key = anchor?.let { remoteKeysDao.remoteKeyByMovieId(it.movie.id) }
                    key?.nextKey?.minus(1) ?: 1
                }

                LoadType.PREPEND -> {
                    // We don’t support “load newer than page 1” for this feed.
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val last =
                        state.lastItemOrNull()
                            ?: return MediatorResult.Success(endOfPaginationReached = true)

                    val key =
                        remoteKeysDao.remoteKeyByMovieId(last.movie.id)
                            ?: return MediatorResult.Success(endOfPaginationReached = true)

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

                db.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        remoteKeysDao.clearRemoteKeys()
                        movieDao.clearCatalogRanks()
                    }

                    val ids = incoming.map { it.id }
                    val existingById = movieDao.getMoviesByIds(ids).associateBy { it.id }

                    val baseRank = (page - 1) * state.config.pageSize

                    val entities =
                        incoming.mapIndexed { index, movie ->
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
                            )
                        }

                    movieDao.upsertAll(entities)

                    val keys =
                        incoming.map { movie ->
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
                                lastUpdatedEpochMillis = System.currentTimeMillis(),
                            )
                        )
                    }
                }

                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
        }
    }
}

private fun PagingState<Int, MovieWithFavorite>.lastItemOrNull(): MovieWithFavorite? =
    pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
