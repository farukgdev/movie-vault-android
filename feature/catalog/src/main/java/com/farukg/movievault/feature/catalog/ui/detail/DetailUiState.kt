package com.farukg.movievault.feature.catalog.ui.detail

import com.farukg.movievault.core.error.AppError

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Error(val error: AppError) : DetailUiState

    data class Content(
        val title: String,
        val genres: List<String>,
        val releaseYear: Int?,
        val rating: Double?,
        val runtimeMinutes: Int?,
        val overview: String,
        val posterUrl: String?,
        val posterFallbackUrl: String?,
        val isFavorite: Boolean,
    ) : DetailUiState
}
