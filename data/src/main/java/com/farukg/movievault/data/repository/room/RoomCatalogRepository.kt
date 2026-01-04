package com.farukg.movievault.data.repository.room

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.cache.CachePolicy
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.CatalogRemoteKeysDao
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.MovieEntity
import com.farukg.movievault.data.local.mapper.toDomainDetail
import com.farukg.movievault.data.local.model.CatalogMovieRow
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.paging.CatalogRemoteMediator
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.data.remote.tmdb.TmdbImageSize
import com.farukg.movievault.data.remote.tmdb.tmdbWithSizeOrNull
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.data.repository.MovieDetailCacheState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RoomCatalogRepository
@Inject
constructor(
    private val db: MovieVaultDatabase,
    private val movieDao: MovieDao,
    private val favoriteDao: FavoriteDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val remote: CatalogRemoteDataSource,
    private val remoteKeysDao: CatalogRemoteKeysDao,
    private val clock: Clock,
) : CatalogRepository {

    private companion object {
        const val TMDB_PAGE_SIZE = 20
        const val NOT_IN_CATALOG_RANK = -1
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun catalogPaging(): Flow<PagingData<Movie>> =
        Pager(
                config =
                    PagingConfig(
                        pageSize = TMDB_PAGE_SIZE,
                        initialLoadSize = TMDB_PAGE_SIZE,
                        prefetchDistance = TMDB_PAGE_SIZE / 2,
                        enablePlaceholders = true,
                    ),
                remoteMediator =
                    CatalogRemoteMediator(
                        db = db,
                        movieDao = movieDao,
                        remoteKeysDao = remoteKeysDao,
                        cacheMetadataDao = cacheMetadataDao,
                        remote = remote,
                        clock = clock,
                    ),
                pagingSourceFactory = { movieDao.catalogPagingSource() },
            )
            .flow
            .map { pagingData ->
                pagingData.map { row: CatalogMovieRow ->
                    Movie(
                        id = row.id,
                        title = row.title,
                        releaseYear = row.releaseYear,
                        posterUrl = row.posterUrl.tmdbWithSizeOrNull(TmdbImageSize.List),
                        rating = row.rating,
                    )
                }
            }

    override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> =
        combine(movieDao.observeMovie(movieId), favoriteDao.observeFavoriteIds()) { entity, favIds
            ->
            val favSet = favIds.toHashSet()
            if (entity == null) {
                AppResult.Error(AppError.Http(code = 404))
            } else {
                AppResult.Success(entity.toDomainDetail(isFavorite = favSet.contains(movieId)))
            }
        }

    override suspend fun refreshMovieDetail(movieId: Long): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val existing = movieDao.getMovie(movieId)
            val rank = existing?.popularRank ?: NOT_IN_CATALOG_RANK

            when (val fetched = remote.fetchMovieDetail(movieId)) {
                is AppResult.Success -> {
                    val merged =
                        fetched.data.toEntity(
                            popularRank = rank,
                            detailFetchedAtEpochMillis = clock.now(),
                        )
                    movieDao.upsert(merged)
                    AppResult.Success(Unit)
                }
                is AppResult.Error -> AppResult.Error(fetched.error)
            }
        }

    override suspend fun movieDetailCacheState(movieId: Long): MovieDetailCacheState =
        withContext(Dispatchers.IO) {
            val entity =
                movieDao.getMovie(movieId) ?: return@withContext MovieDetailCacheState.Missing
            if (entity.detailFetchedAtEpochMillis != null) MovieDetailCacheState.Fetched
            else MovieDetailCacheState.Partial
        }

    override fun catalogLastUpdatedEpochMillis(): Flow<Long?> =
        cacheMetadataDao.observeLastUpdated(CacheKeys.CATALOG_LAST_UPDATED)

    override suspend fun isCatalogStale(nowEpochMillis: Long): Boolean =
        withContext(Dispatchers.IO) {
            val lastUpdated =
                cacheMetadataDao.get(CacheKeys.CATALOG_LAST_UPDATED)?.lastUpdatedEpochMillis
            CachePolicy.isCatalogStale(lastUpdated, nowEpochMillis)
        }

    override suspend fun hasCatalogCache(): Boolean =
        withContext(Dispatchers.IO) { movieDao.countCatalogMovies() > 0 }

    private fun MovieDetail.toEntity(
        popularRank: Int,
        detailFetchedAtEpochMillis: Long?,
    ): MovieEntity =
        MovieEntity(
            id = id,
            title = title,
            releaseYear = releaseYear,
            posterUrl = posterUrl,
            rating = rating,
            overview = overview,
            runtimeMinutes = runtimeMinutes,
            genres = genres,
            popularRank = popularRank,
            detailFetchedAtEpochMillis = detailFetchedAtEpochMillis,
        )
}
