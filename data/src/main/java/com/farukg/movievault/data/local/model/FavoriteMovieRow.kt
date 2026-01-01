package com.farukg.movievault.data.local.model

data class FavoriteMovieRow(
    val id: Long,
    val title: String,
    val releaseYear: Int?,
    val posterUrl: String?,
    val rating: Double?,
    val createdAtEpochMillis: Long,
)
