package com.farukg.movievault.data.repository.room

import androidx.room.withTransaction
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.result.map
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.cache.CachePolicy
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import com.farukg.movievault.data.local.entity.MovieEntity
import com.farukg.movievault.data.local.mapper.hasDetailFields
import com.farukg.movievault.data.local.mapper.toDomainDetail
import com.farukg.movievault.data.local.mapper.toDomainMovie
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.data.repository.CatalogRefreshState
import com.farukg.movievault.data.repository.CatalogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
) : CatalogRepository {

    private data class RefreshInternalState(
        val isRefreshing: Boolean = false,
        val lastError: AppError? = null,
    )

    private val refreshMutex = Mutex()
    private val refreshState = MutableStateFlow(RefreshInternalState())

    override fun catalog(): Flow<AppResult<List<Movie>>> =
        combine(movieDao.observeCatalog(), favoriteDao.observeFavoriteIds()) { entities, favIds ->
            val favSet = favIds.toHashSet()
            val movies = entities.map { it.toDomainMovie(isFavorite = favSet.contains(it.id)) }
            AppResult.Success(movies)
        }

    override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> = channelFlow {
        val existing = withContext(Dispatchers.IO) { movieDao.getMovie(movieId) }

        if (existing == null) {
            // No cached row
            when (val fetched = withContext(Dispatchers.IO) { remote.fetchMovieDetail(movieId) }) {
                is AppResult.Success -> {
                    val merged = fetched.data.toEntity(popularRank = Int.MAX_VALUE)
                    withContext(Dispatchers.IO) { movieDao.upsert(merged) }
                }
                is AppResult.Error -> {
                    send(AppResult.Error(fetched.error))
                    return@channelFlow
                }
            }
        } else if (!existing.hasDetailFields()) {
            // Emit cached row immediately and refresh details in background
            launch(Dispatchers.IO) {
                when (val fetched = remote.fetchMovieDetail(movieId)) {
                    is AppResult.Success -> {
                        val merged = fetched.data.toEntity(popularRank = existing.popularRank)
                        movieDao.upsert(merged)
                    }
                    is AppResult.Error -> {
                        // Non-blocking: keep partial cached row visible
                    }
                }
            }
        }

        launch {
            combine(movieDao.observeMovie(movieId), favoriteDao.observeFavoriteIds()) {
                    entity,
                    favIds ->
                    val favSet = favIds.toHashSet()
                    if (entity == null) {
                        AppResult.Error(AppError.Http(code = 404))
                    } else {
                        AppResult.Success(
                            entity.toDomainDetail(isFavorite = favSet.contains(movieId))
                        )
                    }
                }
                .collect { send(it) }
        }
    }

    override fun catalogRefreshState(): Flow<CatalogRefreshState> =
        cacheMetadataDao.observeLastUpdated(CacheKeys.CATALOG_LAST_UPDATED).combine(refreshState) {
            lastUpdated,
            refresh ->
            CatalogRefreshState(
                lastUpdatedEpochMillis = lastUpdated,
                isRefreshing = refresh.isRefreshing,
                lastRefreshError = refresh.lastError,
            )
        }

    override suspend fun refreshCatalog(force: Boolean): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                val now = System.currentTimeMillis()
                val lastUpdated =
                    cacheMetadataDao.get(CacheKeys.CATALOG_LAST_UPDATED)?.lastUpdatedEpochMillis

                if (!CachePolicy.shouldRefreshCatalog(force, lastUpdated, now)) {
                    return@withLock AppResult.Success(Unit)
                }

                refreshState.value = refreshState.value.copy(isRefreshing = true, lastError = null)

                val remoteResult = remote.fetchPopular(page = 1)

                when (remoteResult) {
                    is AppResult.Error -> {
                        refreshState.value =
                            refreshState.value.copy(
                                isRefreshing = false,
                                lastError = remoteResult.error,
                            )
                        remoteResult.map { Unit }
                    }

                    is AppResult.Success -> {
                        val incoming = remoteResult.data
                        val incomingIds = incoming.map { it.id }
                        val existingById =
                            movieDao.getMoviesByIds(incomingIds).associateBy { it.id }

                        db.withTransaction {
                            val mergedEntities =
                                incoming.mapIndexed { index, movie ->
                                    val existing = existingById[movie.id]
                                    MovieEntity(
                                        id = movie.id,
                                        title = movie.title,
                                        releaseYear = movie.releaseYear,
                                        posterUrl = movie.posterUrl,
                                        rating = movie.rating,
                                        // preserve detail fields if we have them
                                        overview = existing?.overview,
                                        runtimeMinutes = existing?.runtimeMinutes,
                                        genres = existing?.genres ?: emptyList(),
                                        popularRank = index,
                                    )
                                }

                            movieDao.upsertAll(mergedEntities)

                            cacheMetadataDao.upsert(
                                CacheMetadataEntity(
                                    key = CacheKeys.CATALOG_LAST_UPDATED,
                                    lastUpdatedEpochMillis = now,
                                )
                            )
                        }

                        refreshState.value =
                            refreshState.value.copy(isRefreshing = false, lastError = null)
                        AppResult.Success(Unit)
                    }
                }
            }
        }

    private fun MovieDetail.toEntity(popularRank: Int): MovieEntity =
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
        )
}
