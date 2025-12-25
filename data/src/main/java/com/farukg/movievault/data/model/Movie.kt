package com.farukg.movievault.data.model

data class Movie(
    val id: Long,
    val title: String,
    val releaseYear: Int? = null,
    val posterUrl: String? = null,
    val rating: Double? = null,
    val isFavorite: Boolean = false,
)
