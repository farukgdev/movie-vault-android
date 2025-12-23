package com.farukg.movievault.feature.favorites.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FavoritesRouteScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    FavoritesScreen(
        uiState = FavoritesUiState.Empty,
        onRetry = {},
        onBack = onBack,
        modifier = modifier,
    )
}
