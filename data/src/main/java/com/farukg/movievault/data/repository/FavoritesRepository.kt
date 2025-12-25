package com.farukg.movievault.data.repository

import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun favorites(): Flow<AppResult<List<Movie>>>

    /** @return Success(true) if the movie is now favorited, Success(false) if unfavorited. */
    suspend fun toggleFavorite(movieId: Long): AppResult<Boolean>
}
