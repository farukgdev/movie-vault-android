package com.farukg.movievault.data.local.mapper

import com.farukg.movievault.data.local.entity.MovieEntity
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail

internal fun MovieEntity.toDomainMovie(isFavorite: Boolean): Movie =
    Movie(
        id = id,
        title = title,
        releaseYear = releaseYear,
        posterUrl = posterUrl,
        rating = rating,
        isFavorite = isFavorite,
    )

internal fun MovieEntity.toDomainDetail(isFavorite: Boolean): MovieDetail =
    MovieDetail(
        id = id,
        title = title,
        releaseYear = releaseYear,
        genres = genres,
        rating = rating,
        overview = overview.orEmpty(),
        runtimeMinutes = runtimeMinutes,
        posterUrl = posterUrl,
        isFavorite = isFavorite,
        detailFetchedAtEpochMillis = detailFetchedAtEpochMillis,
    )

internal fun MovieEntity.hasDetailFields(): Boolean =
    !overview.isNullOrBlank() || runtimeMinutes != null || genres.isNotEmpty()
