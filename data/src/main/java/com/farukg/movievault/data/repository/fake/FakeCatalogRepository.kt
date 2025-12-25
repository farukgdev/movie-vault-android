package com.farukg.movievault.data.repository.fake

import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeCatalogRepository(private val store: FakeMovieStore) : CatalogRepository {
    override fun catalog(): Flow<AppResult<List<Movie>>> =
        store.catalogFlow().map { AppResult.Success(it) }

    override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> =
        store.movieDetailFlow(movieId).map { detail ->
            if (detail != null) AppResult.Success(detail)
            else AppResult.Error(AppError.Http(code = 404))
        }
}
