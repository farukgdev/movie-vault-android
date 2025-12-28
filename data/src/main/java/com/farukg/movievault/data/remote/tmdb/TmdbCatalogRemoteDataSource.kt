package com.farukg.movievault.data.remote.tmdb

import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.result.map
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.data.remote.safeApiCall
import com.farukg.movievault.data.remote.tmdb.mapper.toDomain

class TmdbCatalogRemoteDataSource(
    private val api: TmdbApiService,
    private val imageBaseUrl: String,
) : CatalogRemoteDataSource {

    override suspend fun fetchPopularPage(page: Int): AppResult<MoviesPage> =
        safeApiCall { api.popularMovies(page) }
            .map { dto ->
                MoviesPage(
                    page = dto.page,
                    totalPages = dto.totalPages,
                    results = dto.results.map { it.toDomain(imageBaseUrl) },
                )
            }

    override suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail> =
        safeApiCall { api.movieDetail(movieId) }.map { it.toDomain(imageBaseUrl) }
}
