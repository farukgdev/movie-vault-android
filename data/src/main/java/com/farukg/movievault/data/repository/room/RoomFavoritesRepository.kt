package com.farukg.movievault.data.repository.room

import androidx.room.withTransaction
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.FavoriteEntity
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Singleton
class RoomFavoritesRepository
@Inject
constructor(private val db: MovieVaultDatabase, private val favoriteDao: FavoriteDao) :
    FavoritesRepository {

    override fun favorites(): Flow<AppResult<List<Movie>>> =
        favoriteDao
            .observeFavoriteRows()
            .map { rows ->
                val movies =
                    rows.map { row ->
                        Movie(
                            id = row.id,
                            title = row.title,
                            releaseYear = row.releaseYear,
                            posterUrl = row.posterUrl,
                            rating = row.rating,
                            isFavorite = true,
                        )
                    }
                AppResult.Success(movies)
            }
            .flowOn(Dispatchers.Default)

    override suspend fun toggleFavorite(movieId: Long): AppResult<Boolean> =
        try {
            val now = System.currentTimeMillis()
            val newState =
                db.withTransaction {
                    if (favoriteDao.isFavorited(movieId)) {
                        favoriteDao.delete(movieId)
                        false
                    } else {
                        favoriteDao.insert(
                            FavoriteEntity(movieId = movieId, createdAtEpochMillis = now)
                        )
                        true
                    }
                }
            AppResult.Success(newState)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            AppResult.Error(AppError.Unknown(t))
        }
}
