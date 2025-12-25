package com.farukg.movievault.data.repository.fake

import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeFavoritesRepository(private val store: FakeMovieStore) : FavoritesRepository {
    override fun favorites(): Flow<AppResult<List<Movie>>> =
        store.favoritesFlow().map { AppResult.Success(it) }

    override suspend fun toggleFavorite(movieId: Long): AppResult<Boolean> =
        AppResult.Success(store.toggleFavorite(movieId))
}
