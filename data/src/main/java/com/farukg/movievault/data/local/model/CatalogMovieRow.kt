package com.farukg.movievault.data.local.model

data class CatalogMovieRow(
    val id: Long,
    val title: String,
    val releaseYear: Int?,
    val posterUrl: String?,
    val rating: Double?,
    val popularRank: Int,
    val isFavorite: Boolean,
)
