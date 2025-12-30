package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CatalogRouteScreen(
    onOpenDetail: (movieId: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val movies = viewModel.moviesPaging.collectAsLazyPagingItems()

    val savedScroll by viewModel.scrollPosition.collectAsStateWithLifecycle()
    val initialScroll = remember { savedScroll }

    val listState: LazyListState = remember {
        LazyListState(
            firstVisibleItemIndex = initialScroll.index,
            firstVisibleItemScrollOffset = initialScroll.offset,
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collectLatest { (index, offset) -> viewModel.onScrollPositionChanged(index, offset) }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nearTop = listState.firstVisibleItemIndex <= 8
                viewModel.onResumed(canAutoRefresh = nearTop)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    suspend fun scrollToTopSmart() {
        if (listState.firstVisibleItemIndex > 20) {
            listState.scrollToItem(0)
        } else {
            listState.animateScrollToItem(0)
        }
    }
    var pendingScrollOrigin by remember { mutableStateOf<RefreshOrigin?>(null) }
    var sawLoadingForPending by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.refreshRequests.collectLatest { origin ->
            pendingScrollOrigin = origin
            sawLoadingForPending = false
            movies.refresh()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { movies.loadState to movies.itemCount }
            .collectLatest { (loadState, count) ->
                val refreshState = loadState.mediator?.refresh ?: loadState.refresh
                val isLoading = refreshState is LoadState.Loading
                val error = (refreshState as? LoadState.Error)?.error?.toAppError()

                viewModel.onPagingRefreshSnapshot(
                    isLoading = isLoading,
                    error = error,
                    hasItems = count > 0,
                )
                // pending refresh request: only scroll when it is successful
                val pending = pendingScrollOrigin
                if (pending != null) {
                    if (refreshState is LoadState.Loading) {
                        sawLoadingForPending = true
                    } else if (sawLoadingForPending) {
                        val success = refreshState is LoadState.NotLoading
                        if (success) {
                            val shouldScroll =
                                when (pending) {
                                    RefreshOrigin.Manual -> true
                                    RefreshOrigin.Automatic -> listState.firstVisibleItemIndex <= 8
                                }
                            if (shouldScroll) scrollToTopSmart()
                        }
                        pendingScrollOrigin = null
                        sawLoadingForPending = false
                    }
                }
            }
    }

    val statusUi by viewModel.statusUi.collectAsStateWithLifecycle()
    val manualRefreshing by viewModel.manualRefreshing.collectAsStateWithLifecycle()
    val everHadItems by viewModel.everHadItems.collectAsStateWithLifecycle()

    CatalogScreen(
        movies = movies,
        listState = listState,
        statusUi = statusUi,
        refreshEvents = viewModel.refreshEvents,
        isManualRefreshing = manualRefreshing,
        everHadItems = everHadItems,
        onRetry = { movies.retry() },
        onRefresh = viewModel::requestManualRefresh,
        onOpenDetail = onOpenDetail,
        onOpenFavorites = onOpenFavorites,
        modifier = modifier,
    )
}
