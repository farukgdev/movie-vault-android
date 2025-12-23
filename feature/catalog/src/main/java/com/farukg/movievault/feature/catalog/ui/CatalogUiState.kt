package com.farukg.movievault.feature.catalog.ui

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Error(val message: String? = null) : CatalogUiState

    data object Empty : CatalogUiState

    data class Content(val movies: List<MovieRowUi>) : CatalogUiState
}

data class MovieRowUi(val id: Long, val title: String, val subtitle: String)
