package com.farukg.movievault.feature.catalog.ui.detail

import com.farukg.movievault.core.error.AppError

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Error(val error: AppError) : DetailUiState

    data class Content(
        val title: String,
        val metaPrimary: String,
        val metaSecondary: String,
        val overview: String,
        val posterUrl: String?,
        val isFavorite: Boolean,
    ) : DetailUiState
}
