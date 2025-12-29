package com.farukg.movievault.data.repository

import androidx.paging.PagingData
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import kotlinx.coroutines.flow.Flow

data class CatalogRefreshState(
    val lastUpdatedEpochMillis: Long?,
    val isRefreshing: Boolean,
    val lastRefreshError: AppError?,
)

interface CatalogRepository {
    fun catalog(): Flow<AppResult<List<Movie>>>

    fun catalogPaging(): Flow<PagingData<Movie>>

    fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>>

    fun catalogRefreshState(): Flow<CatalogRefreshState>

    suspend fun refreshCatalog(force: Boolean = false): AppResult<Unit>
}
