package com.farukg.movievault.testing

import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class FakeCatalogRemoteDataSource : CatalogRemoteDataSource {

    private val popularByPage = ConcurrentHashMap<Int, ArrayDeque<AppResult<MoviesPage>>>()
    private val detailById = ConcurrentHashMap<Long, ArrayDeque<AppResult<MovieDetail>>>()

    fun enqueuePopular(page: Int, result: AppResult<MoviesPage>) {
        popularByPage.getOrPut(page) { ArrayDeque() }.addLast(result)
    }

    fun enqueueDetail(movieId: Long, result: AppResult<MovieDetail>) {
        detailById.getOrPut(movieId) { ArrayDeque() }.addLast(result)
    }

    fun reset() {
        popularByPage.clear()
        detailById.clear()
    }

    override suspend fun fetchPopularPage(page: Int): AppResult<MoviesPage> {
        val q = popularByPage[page]
        return q?.removeFirstOrNull()
            ?: AppResult.Error(
                AppError.Unknown(IllegalStateException("No fake response for popular page=$page"))
            )
    }

    override suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail> {
        val q = detailById[movieId]
        return q?.removeFirstOrNull()
            ?: AppResult.Error(
                AppError.Unknown(IllegalStateException("No fake response for detail id=$movieId"))
            )
    }

    private fun <T> ArrayDeque<T>.removeFirstOrNull(): T? = if (isEmpty()) null else removeFirst()
}
