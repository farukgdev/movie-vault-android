package com.farukg.movievault.data.remote.tmdb.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoviesPageDto(
    val page: Int = 1,
    val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class MovieDto(
    val id: Long,
    val title: String,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)

@Serializable
data class MovieDetailDto(
    val id: Long,
    val title: String,
    @SerialName("release_date") val releaseDate: String? = null,
    val genres: List<GenreDto> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable data class GenreDto(val id: Int, val name: String)
