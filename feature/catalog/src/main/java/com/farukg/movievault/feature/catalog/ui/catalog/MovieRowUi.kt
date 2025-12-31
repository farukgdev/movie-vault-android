package com.farukg.movievault.feature.catalog.ui.catalog

data class MovieRowUi(
    val id: Long,
    val title: String,
    val releaseYear: Int?,
    val rating: Double?,
    val posterUrl: String?,
)
