package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.core.ui.EmptyState
import com.farukg.movievault.core.ui.ErrorState
import com.farukg.movievault.core.ui.components.MetaPill
import com.farukg.movievault.core.ui.components.MoviePoster
import com.farukg.movievault.core.ui.components.MovieVaultCard
import com.farukg.movievault.core.ui.components.RatingPill
import com.farukg.movievault.feature.catalog.ui.components.catalogPosterHeightDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

private const val EMPTY_DEBOUNCE_MS = 300L
private const val AUTO_RETRY_THROTTLE_MS = 3_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    movies: LazyPagingItems<MovieRowUi>,
    listState: LazyListState,
    statusUi: CatalogStatusUi,
    refreshEvents: Flow<AppError>,
    isManualRefreshing: Boolean,
    everHadItems: Boolean,
    fullScreenError: AppError?,
    onRetryInitialLoad: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (movieId: Long, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    val manualRefreshingUi = rememberMinManualRefreshIndicatorDuration(isManualRefreshing)

    // prefer mediator load states when available
    val mediator = movies.loadState.mediator
    val refresh = mediator?.refresh ?: movies.loadState.refresh
    val append = mediator?.append ?: movies.loadState.append

    val hasItemsNow = movies.itemCount > 0

    val emptyCandidate =
        (refresh is LoadState.NotLoading) && !hasItemsNow && !everHadItems && statusUi.error == null

    val showEmpty = rememberDelayedTrue(target = emptyCandidate, delayMs = EMPTY_DEBOUNCE_MS)

    LaunchedEffect(refreshEvents) {
        refreshEvents.collectLatest { err ->
            val result =
                snackbarHostState.showSnackbar(
                    message = err.toRefreshSnackbarMessage(),
                    actionLabel = "Retry",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) onRefresh()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            isRefreshing = manualRefreshingUi,
            onRefresh = { if (!isManualRefreshing) onRefresh() },
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = manualRefreshingUi,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            when {
                fullScreenError != null -> {
                    ErrorState(
                        modifier = Modifier.fillMaxSize(),
                        message = fullScreenError.userMessage(),
                        onRetry = onRetryInitialLoad,
                    )
                }

                // (e.g. when coming back from Detail)
                !hasItemsNow && everHadItems -> {
                    CatalogSkeletonList(modifier = Modifier.fillMaxSize())
                }

                showEmpty -> {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        title = "No movies yet",
                        message = "Pull down to refresh.",
                        actionLabel = "Refresh",
                        onAction = onRefresh,
                    )
                }

                !hasItemsNow -> {
                    CatalogSkeletonList(modifier = Modifier.fillMaxSize())
                }

                else -> {
                    CatalogList(
                        movies = movies,
                        append = append,
                        listState = listState,
                        onOpenDetail = onOpenDetail,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) { data ->
            MovieVaultSnackbar(data = data, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun rememberDelayedTrue(target: Boolean, delayMs: Long): Boolean {
    var value by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (!target) value = false
        else {
            delay(delayMs)
            value = true
        }
    }
    return value
}

@Composable
private fun CatalogSkeletonList(modifier: Modifier = Modifier, count: Int = 10) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(count, key = { "skeleton_$it" }) { MovieRowPlaceholder() }
    }
}

@Composable
private fun CatalogList(
    movies: LazyPagingItems<MovieRowUi>,
    append: LoadState,
    listState: LazyListState,
    onOpenDetail: (movieId: Long, title: String) -> Unit,
) {
    var stickyAppendError by remember { mutableStateOf<AppError?>(null) }
    var stickyAtCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(append) {
        if (append is LoadState.Error) {
            stickyAppendError = append.error.toAppError()
            stickyAtCount = movies.itemCount
        }
    }

    LaunchedEffect(movies.itemCount) {
        if (movies.itemCount > stickyAtCount) stickyAppendError = null
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = movies.itemCount,
            key = movies.itemKey { it.id },
            contentType = movies.itemContentType { "movie" },
        ) { index ->
            val row = movies[index]
            if (row == null) {
                MovieRowPlaceholder()
            } else {
                MovieRow(
                    title = row.title,
                    releaseYear = row.releaseYear,
                    rating = row.rating,
                    posterUrl = row.posterUrl,
                    onClick = { onOpenDetail(row.id, row.title) },
                )
            }
        }

        item(key = "append_footer", contentType = "footer") {
            AppendFooter(
                append = append,
                stickyError = stickyAppendError,
                onRetry = { movies.retry() },
            )
        }
    }

    AutoRetryAppendOnScroll(movies = movies, append = append, listState = listState)
}

@Composable
private fun AppendFooter(
    append: LoadState,
    stickyError: AppError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.fillMaxWidth().animateContentSize().padding(16.dp).heightIn(min = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            append is LoadState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Loading more…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            append is LoadState.Error -> {
                val err = append.error.toAppError()
                AppendErrorBlock(err = err, onRetry = onRetry)
            }

            stickyError != null -> {
                AppendErrorBlock(err = stickyError, onRetry = onRetry)
            }

            else -> {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppendErrorBlock(err: AppError, onRetry: () -> Unit) {
    val message =
        if (err is AppError.Offline) {
            "Offline — can't load more movies."
        } else {
            err.userMessage()
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun AutoRetryAppendOnScroll(
    movies: LazyPagingItems<MovieRowUi>,
    append: LoadState,
    listState: LazyListState,
) {
    var lastAutoRetryAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(movies, listState, append) {
        snapshotFlow {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val isScrolling = listState.isScrollInProgress
                lastVisible to isScrolling
            }
            .collectLatest { (lastVisible, isScrolling) ->
                if (isScrolling) return@collectLatest
                val count = movies.itemCount
                if (count == 0) return@collectLatest

                val nearEnd = lastVisible >= (count - 2)
                val offlineErr =
                    (append as? LoadState.Error)?.error?.toAppError() as? AppError.Offline
                if (!nearEnd || offlineErr == null) return@collectLatest

                val now = System.currentTimeMillis()
                if (now - lastAutoRetryAt < AUTO_RETRY_THROTTLE_MS) return@collectLatest

                lastAutoRetryAt = now
                movies.retry()
            }
    }
}

@Composable
private fun MovieRow(
    title: String,
    releaseYear: Int?,
    rating: Double?,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MovieVaultCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val posterHeight = catalogPosterHeightDp()

            MoviePoster(
                posterUrl = posterUrl,
                contentDescription = "$title poster",
                modifier = Modifier.height(posterHeight).aspectRatio(2f / 3f),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (releaseYear != null || rating != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        releaseYear?.let { MetaPill(text = it.toString()) }
                        rating?.let { RatingPill(rating = it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieRowPlaceholder(modifier: Modifier = Modifier) {
    MovieVaultCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val posterHeight = catalogPosterHeightDp()
            Spacer(
                modifier =
                    Modifier.height(posterHeight)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Spacer(
                    Modifier.fillMaxWidth(0.6f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(10.dp))
                Spacer(
                    Modifier.fillMaxWidth(0.45f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun rememberMinManualRefreshIndicatorDuration(
    isRefreshing: Boolean,
    minDurationMs: Long = 500L,
): Boolean {
    var uiRefreshing by remember { mutableStateOf(false) }
    var lastStartAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            lastStartAt = System.currentTimeMillis()
            uiRefreshing = true
        } else {
            val elapsed = System.currentTimeMillis() - lastStartAt
            val remaining = minDurationMs - elapsed
            if (remaining > 0) delay(remaining)
            uiRefreshing = false
        }
    }

    return uiRefreshing
}

@Composable
private fun MovieVaultSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            data.visuals.actionLabel?.let { label ->
                TextButton(
                    onClick = { data.performAction() },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.inversePrimary
                        ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(text = label)
                }
            }

            if (data.visuals.withDismissAction) {
                IconButton(onClick = { data.dismiss() }, modifier = Modifier.size(34.dp)) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Dismiss")
                }
            }
        }
    }
}

private fun AppError.toRefreshSnackbarMessage(): String =
    when (this) {
        is AppError.Offline -> "Offline"
        is AppError.Network -> "Network error"
        is AppError.Http -> "Server error"
        is AppError.Serialization -> "Bad response"
        is AppError.Unknown -> "Couldn't refresh"
    }
