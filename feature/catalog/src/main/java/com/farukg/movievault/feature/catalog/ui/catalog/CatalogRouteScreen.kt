package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.ui.scaffold.RegisterTopBar
import com.farukg.movievault.core.ui.testing.TestTags
import com.farukg.movievault.feature.catalog.navigation.CatalogRoute
import com.farukg.movievault.feature.catalog.ui.catalog.topbar.CatalogStatusSheetContent
import com.farukg.movievault.feature.catalog.ui.catalog.topbar.CatalogTopAppBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogRouteScreen(
    onOpenDetail: (movieId: Long, title: String) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val movies = viewModel.moviesPaging.collectAsLazyPagingItems()
    val fullScreenError by viewModel.fullScreenError.collectAsStateWithLifecycle()

    val savedScroll by viewModel.scrollPosition.collectAsStateWithLifecycle()
    val initialScroll = remember { savedScroll }

    val gridState: LazyGridState = remember {
        LazyGridState(
            firstVisibleItemIndex = initialScroll.index,
            firstVisibleItemScrollOffset = initialScroll.offset,
        )
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collectLatest { (index, offset) -> viewModel.onScrollPositionChanged(index, offset) }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nearTop = gridState.firstVisibleItemIndex <= 8
                viewModel.onResumed(canAutoRefresh = nearTop)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    var showStatusSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val statusUi by viewModel.statusUi.collectAsStateWithLifecycle()

    RegisterTopBar(CatalogRoute) {
        CatalogTopAppBar(
            statusUi = statusUi,
            onOpenStatus = { showStatusSheet = true },
            onOpenFavorites = onOpenFavorites,
        )
    }

    suspend fun scrollToTopSmart() {
        if (gridState.firstVisibleItemIndex > 12) {
            gridState.scrollToItem(0)
        } else {
            gridState.animateScrollToItem(0)
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
                val sig = computeRefreshSignals(loadState)

                viewModel.onPagingRefreshSnapshot(
                    uiLoading = sig.uiLoading,
                    attemptLoading = sig.attemptLoading,
                    error = sig.attemptError,
                    hasItems = count > 0,
                )

                // scroll to top only after the refresh succeeds
                pendingScrollOrigin?.let { pending ->
                    if (sig.attemptLoading) {
                        sawLoadingForPending = true
                    } else if (sawLoadingForPending) {
                        if (sig.attemptSuccess) {
                            val shouldScroll =
                                when (pending) {
                                    RefreshOrigin.Manual -> true
                                    RefreshOrigin.Automatic -> gridState.firstVisibleItemIndex <= 8
                                }
                            if (shouldScroll) scrollToTopSmart()
                        }
                        pendingScrollOrigin = null
                        sawLoadingForPending = false
                    }
                }
            }
    }

    val manualRefreshing by viewModel.manualRefreshing.collectAsStateWithLifecycle()
    val everHadItems by viewModel.everHadItems.collectAsStateWithLifecycle()

    CatalogScreen(
        movies = movies,
        gridState = gridState,
        statusUi = statusUi,
        refreshEvents = viewModel.refreshEvents,
        isManualRefreshing = manualRefreshing,
        everHadItems = everHadItems,
        fullScreenError = fullScreenError,
        onRetryInitialLoad = viewModel::retryFromFullScreenError,
        onRefresh = viewModel::requestManualRefresh,
        onOpenDetail = onOpenDetail,
        modifier = modifier,
    )

    if (showStatusSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    showStatusSheet = false
                }
            },
            sheetState = sheetState,
            modifier = Modifier.testTag(TestTags.STATUS_SHEET),
        ) {
            CatalogStatusSheetContent(
                statusUi = statusUi,
                onRefreshNow = {
                    viewModel.requestManualRefresh()
                    scope.launch {
                        sheetState.hide()
                        showStatusSheet = false
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showStatusSheet = false
                    }
                },
            )
        }
    }
}

private data class RefreshSignals(
    val uiLoading: Boolean,
    val attemptLoading: Boolean,
    val attemptError: AppError?,
    val attemptSuccess: Boolean,
)

private fun computeRefreshSignals(loadState: CombinedLoadStates): RefreshSignals {
    // source.refresh: Room query (DB load/invalidation)
    // mediator.refresh: network refresh attempt (RemoteMediator)
    // do not treat source refresh transitions as "refresh attempt finished"

    val source = loadState.refresh
    val mediator = loadState.mediator?.refresh

    // user-perceived loading
    val uiLoading = (source is LoadState.Loading) || (mediator is LoadState.Loading)

    val attempt = mediator ?: source
    val attemptLoading = attempt is LoadState.Loading
    val attemptError = (attempt as? LoadState.Error)?.error?.toAppError()
    val attemptSuccess = (attempt is LoadState.NotLoading) && attemptError == null

    return RefreshSignals(
        uiLoading = uiLoading,
        attemptLoading = attemptLoading,
        attemptError = attemptError,
        attemptSuccess = attemptSuccess,
    )
}
