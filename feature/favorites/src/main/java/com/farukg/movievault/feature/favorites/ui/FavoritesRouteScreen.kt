package com.farukg.movievault.feature.favorites.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FavoritesRouteScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FavoritesViewModel = hiltViewModel()
    val uiState by
        viewModel.uiState.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)

    FavoritesScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onBack = onBack,
        onOpenDetail = onOpenDetail,
        onToggleFavorite = viewModel::toggleFavorite,
        modifier = modifier,
    )
}
