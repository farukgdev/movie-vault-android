package com.farukg.movievault.data.remote.tmdb.mapper

import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.remote.tmdb.dto.MovieDetailDto
import com.farukg.movievault.data.remote.tmdb.dto.MovieDto

internal fun MovieDto.toDomain(imageBaseUrl: String): Movie =
    Movie(
        id = id,
        title = title,
        releaseYear = releaseDate?.take(4)?.toIntOrNull(),
        posterUrl = buildPosterUrl(imageBaseUrl, posterPath),
        rating = voteAverage,
        isFavorite = false, // remote doesn’t know; repository will merge later
    )

internal fun MovieDetailDto.toDomain(imageBaseUrl: String): MovieDetail =
    MovieDetail(
        id = id,
        title = title,
        releaseYear = releaseDate?.take(4)?.toIntOrNull(),
        genres = genres.map { it.name },
        rating = voteAverage,
        overview = overview.orEmpty(),
        runtimeMinutes = runtime,
        posterUrl = buildPosterUrl(imageBaseUrl, posterPath),
        isFavorite = false,
    )

private fun buildPosterUrl(imageBaseUrl: String, posterPath: String?): String? {
    val path = posterPath?.trim()?.removePrefix("/") ?: return null
    return imageBaseUrl.trimEnd('/') + "/" + path
}
