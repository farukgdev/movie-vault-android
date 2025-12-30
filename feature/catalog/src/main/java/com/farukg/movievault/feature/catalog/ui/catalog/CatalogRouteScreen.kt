package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CatalogRouteScreen(
    onOpenDetail: (movieId: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val movies = viewModel.moviesPaging.collectAsLazyPagingItems()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.refreshRequests.collectLatest { movies.refresh() } }

    LaunchedEffect(Unit) {
        snapshotFlow { movies.loadState.refresh to movies.itemCount }
            .collectLatest { (refresh, count) ->
                val isLoading = refresh is LoadState.Loading
                val error = (refresh as? LoadState.Error)?.error?.toAppError()
                viewModel.onPagingRefreshSnapshot(
                    isLoading = isLoading,
                    error = error,
                    hasItems = count > 0,
                )
            }
    }

    val statusUi by viewModel.statusUi.collectAsStateWithLifecycle()
    val manualRefreshing by viewModel.manualRefreshing.collectAsStateWithLifecycle()

    CatalogScreen(
        movies = movies,
        statusUi = statusUi,
        refreshEvents = viewModel.refreshEvents,
        isManualRefreshing = manualRefreshing,
        onRetry = { movies.retry() },
        onRefresh = viewModel::requestManualRefresh,
        onOpenDetail = onOpenDetail,
        onOpenFavorites = onOpenFavorites,
        modifier = modifier,
    )
}
