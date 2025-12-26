package com.farukg.movievault.data.test

import com.farukg.movievault.data.local.entity.MovieEntity

internal fun movieEntity(
    id: Long,
    title: String = "Movie $id",
    popularRank: Int = 0,
    releaseYear: Int? = 2024,
    posterUrl: String? = null,
    rating: Double? = 8.0,
    overview: String? = null,
    runtimeMinutes: Int? = null,
    genres: List<String> = emptyList(),
): MovieEntity =
    MovieEntity(
        id = id,
        title = title,
        releaseYear = releaseYear,
        posterUrl = posterUrl,
        rating = rating,
        overview = overview,
        runtimeMinutes = runtimeMinutes,
        genres = genres,
        popularRank = popularRank,
    )
