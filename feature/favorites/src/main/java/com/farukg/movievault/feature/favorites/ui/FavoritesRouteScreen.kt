package com.farukg.movievault.feature.favorites.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farukg.movievault.core.ui.scaffold.RegisterTopBar
import com.farukg.movievault.feature.favorites.navigation.FavoritesRoute
import com.farukg.movievault.feature.favorites.ui.topbar.FavoritesTopAppBar

@Composable
fun FavoritesRouteScreen(
    viewModel: FavoritesViewModel,
    onBack: () -> Unit,
    onOpenDetail: (movieId: Long, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by
        viewModel.uiState.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)

    val savedCount: Int? =
        when (uiState) {
            FavoritesUiState.Loading -> null
            is FavoritesUiState.Content -> (uiState as FavoritesUiState.Content).movies.size
            else -> 0
        }

    RegisterTopBar(FavoritesRoute) { FavoritesTopAppBar(onBack = onBack, savedCount = savedCount) }

    FavoritesScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onOpenDetail = onOpenDetail,
        onToggleFavorite = viewModel::toggleFavorite,
        modifier = modifier,
    )
}
