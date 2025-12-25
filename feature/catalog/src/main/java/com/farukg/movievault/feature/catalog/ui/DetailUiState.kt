package com.farukg.movievault.feature.catalog.ui

import com.farukg.movievault.core.error.AppError

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Error(val error: AppError) : DetailUiState

    data class Content(val title: String, val meta: String, val overview: String) : DetailUiState
}
