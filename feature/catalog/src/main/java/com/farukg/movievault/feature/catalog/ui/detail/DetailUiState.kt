package com.farukg.movievault.feature.catalog.ui.detail

import com.farukg.movievault.core.error.AppError

sealed interface DetailUiState {
    data class Loading(val isRefreshing: Boolean = false) : DetailUiState

    data class NoCacheError(val error: AppError) : DetailUiState

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
        // behavior flags
        val hasFetchedDetail: Boolean,
        val isRefreshing: Boolean,
        val bannerError: AppError?,
        val hasAttemptedRefresh: Boolean,
    ) : DetailUiState
}
