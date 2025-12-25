package com.farukg.movievault.data.remote.tmdb

import com.farukg.movievault.data.remote.tmdb.dto.MovieDetailDto
import com.farukg.movievault.data.remote.tmdb.dto.MoviesPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("movie/popular") suspend fun popularMovies(@Query("page") page: Int = 1): MoviesPageDto

    @GET("movie/{movie_id}")
    suspend fun movieDetail(@Path("movie_id") movieId: Long): MovieDetailDto
}
