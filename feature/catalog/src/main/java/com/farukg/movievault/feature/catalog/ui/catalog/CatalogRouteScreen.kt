package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CatalogRouteScreen(
    onOpenDetail: (movieId: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CatalogViewModel = hiltViewModel()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResumed()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val manualRefreshing by viewModel.manualRefreshing.collectAsStateWithLifecycle()

    CatalogScreen(
        uiState = uiState,
        refreshState = refreshState,
        refreshEvents = viewModel.refreshEvents,
        isManualRefreshing = manualRefreshing,
        onRetry = viewModel::retry,
        onRefresh = viewModel::onUserRefresh,
        onOpenDetail = onOpenDetail,
        onOpenFavorites = onOpenFavorites,
        modifier = modifier,
    )
}
