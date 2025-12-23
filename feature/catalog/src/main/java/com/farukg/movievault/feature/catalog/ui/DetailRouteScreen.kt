package com.farukg.movievault.feature.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun DetailRouteScreen(movieId: Long, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState =
        remember(movieId) {
            DetailUiState.Content(
                title = "Movie #$movieId",
                meta = "Action • 2024 • ★ 8.2",
                overview = "Overview goes here.",
            )
        }

    DetailScreen(
        uiState = uiState,
        movieId = movieId,
        onRetry = {},
        onBack = onBack,
        modifier = modifier,
    )
}
