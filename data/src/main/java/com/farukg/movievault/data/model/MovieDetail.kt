package com.farukg.movievault.data.model

data class MovieDetail(
    val id: Long,
    val title: String,
    val releaseYear: Int? = null,
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val overview: String = "",
    val runtimeMinutes: Int? = null,
    val posterUrl: String? = null,
    val isFavorite: Boolean = false,
    val detailFetchedAtEpochMillis: Long? = null,
)
