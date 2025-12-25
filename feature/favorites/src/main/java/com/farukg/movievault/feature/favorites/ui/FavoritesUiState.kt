package com.farukg.movievault.feature.favorites.ui

import com.farukg.movievault.core.error.AppError

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState

    data class Error(val error: AppError) : FavoritesUiState

    data object Empty : FavoritesUiState
}
