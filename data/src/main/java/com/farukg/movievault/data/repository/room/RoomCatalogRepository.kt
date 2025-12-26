package com.farukg.movievault.data.repository.room

import androidx.room.withTransaction
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.cache.CacheKeys
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
import com.farukg.movievault.data.repository.CatalogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    private val seedMutex = Mutex()

    override fun catalog(): Flow<AppResult<List<Movie>>> = channelFlow {
        val seed = ensureCatalogSeededIfEmpty()
        if (seed is AppResult.Error && movieDao.countMovies() == 0) {
            send(seed)
            return@channelFlow
        }

        launch {
            combine(movieDao.observeCatalog(), favoriteDao.observeFavoriteIds()) { entities, favIds
                    ->
                    val favSet = favIds.toHashSet()
                    entities.map { it.toDomainMovie(isFavorite = favSet.contains(it.id)) }
                }
                .collect { movies -> send(AppResult.Success(movies)) }
        }
    }

    override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> = channelFlow {
        // Fetch detail if missing (but don't block cached partial if we have it)
        val existing = withContext(Dispatchers.IO) { movieDao.getMovie(movieId) }
        if (existing == null || !existing.hasDetailFields()) {
            val fetched = withContext(Dispatchers.IO) { remote.fetchMovieDetail(movieId) }
            when (fetched) {
                is AppResult.Success -> {
                    // If movie isn't from catalog, give huge number so it doesn’t mess ordering
                    val rank = existing?.popularRank ?: Int.MAX_VALUE
                    val merged = fetched.data.toEntity(popularRank = rank)
                    withContext(Dispatchers.IO) { movieDao.upsert(merged) }
                }
                is AppResult.Error -> {
                    if (existing == null) {
                        send(AppResult.Error(fetched.error))
                        return@channelFlow
                    }
                    // else: keep partial cached row visible
                }
            }
        }

        launch {
            combine(movieDao.observeMovie(movieId), favoriteDao.observeFavoriteIds()) {
                    entity,
                    favIds ->
                    val favSet = favIds.toHashSet()
                    if (entity == null) {
                        // treat as "not found"
                        AppResult.Error(AppError.Http(code = 404))
                    } else {
                        AppResult.Success(
                            entity.toDomainDetail(isFavorite = favSet.contains(movieId))
                        )
                    }
                }
                .collect { result -> send(result) }
        }
    }

    private suspend fun ensureCatalogSeededIfEmpty(): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            seedMutex.withLock {
                if (movieDao.countMovies() > 0) return@withLock AppResult.Success(Unit)

                when (val remoteResult = remote.fetchPopular(page = 1)) {
                    is AppResult.Error -> remoteResult
                    is AppResult.Success -> {
                        val incoming = remoteResult.data

                        db.withTransaction {
                            val entities =
                                incoming.mapIndexed { index, movie ->
                                    movie.toEntity(popularRank = index)
                                }
                            movieDao.upsertAll(entities)

                            cacheMetadataDao.upsert(
                                CacheMetadataEntity(
                                    key = CacheKeys.CATALOG_LAST_UPDATED,
                                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                                )
                            )
                        }

                        AppResult.Success(Unit)
                    }
                }
            }
        }

    private fun Movie.toEntity(popularRank: Int): MovieEntity =
        MovieEntity(
            id = id,
            title = title,
            releaseYear = releaseYear,
            posterUrl = posterUrl,
            rating = rating,
            overview = null,
            runtimeMinutes = null,
            genres = emptyList(),
            popularRank = popularRank,
        )

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
