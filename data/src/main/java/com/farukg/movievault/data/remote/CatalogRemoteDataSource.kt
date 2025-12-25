package com.farukg.movievault.data.remote

import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail

interface CatalogRemoteDataSource {
    suspend fun fetchPopular(page: Int = 1): AppResult<List<Movie>>

    suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail>
}
