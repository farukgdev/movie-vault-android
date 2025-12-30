package com.farukg.movievault.data.repository

import androidx.paging.PagingData
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {

    fun catalogPaging(): Flow<PagingData<Movie>>

    fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>>

    fun catalogLastUpdatedEpochMillis(): Flow<Long?>

    suspend fun isCatalogStale(nowEpochMillis: Long): Boolean
}
