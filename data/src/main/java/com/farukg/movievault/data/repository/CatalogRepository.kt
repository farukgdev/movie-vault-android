package com.farukg.movievault.data.repository

import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun catalog(): Flow<AppResult<List<Movie>>>

    fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>>
}
