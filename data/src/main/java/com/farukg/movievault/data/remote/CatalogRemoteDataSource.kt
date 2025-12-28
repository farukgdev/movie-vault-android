package com.farukg.movievault.data.remote

import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage

interface CatalogRemoteDataSource {
    suspend fun fetchPopularPage(page: Int = 1): AppResult<MoviesPage>

    suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail>
}
