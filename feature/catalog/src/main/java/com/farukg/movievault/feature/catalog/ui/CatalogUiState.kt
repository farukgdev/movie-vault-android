package com.farukg.movievault.feature.catalog.ui

import com.farukg.movievault.core.error.AppError

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Error(val error: AppError) : CatalogUiState

    data object Empty : CatalogUiState

    data class Content(val movies: List<MovieRowUi>) : CatalogUiState
}

data class MovieRowUi(val id: Long, val title: String, val subtitle: String)
