package com.farukg.movievault.feature.catalog.ui

sealed interface DetailUiState {
    data object Loading : DetailUiState

    data class Error(val message: String? = null) : DetailUiState

    data class Content(val title: String, val meta: String, val overview: String) : DetailUiState
}
