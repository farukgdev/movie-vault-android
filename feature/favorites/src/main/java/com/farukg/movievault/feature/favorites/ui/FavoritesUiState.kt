package com.farukg.movievault.feature.favorites.ui

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState

    data class Error(val message: String? = null) : FavoritesUiState

    data object Empty : FavoritesUiState
}
