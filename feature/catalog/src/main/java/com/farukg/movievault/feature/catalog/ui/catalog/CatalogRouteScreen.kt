package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CatalogRouteScreen(
    onOpenDetail: (movieId: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CatalogScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onOpenDetail = onOpenDetail,
        onOpenFavorites = onOpenFavorites,
        modifier = modifier,
    )
}
