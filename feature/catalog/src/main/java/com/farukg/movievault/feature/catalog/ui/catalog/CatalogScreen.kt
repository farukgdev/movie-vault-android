package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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
import com.farukg.movievault.core.ui.testing.TestTags
import com.farukg.movievault.data.remote.tmdb.TmdbImageSize
import com.farukg.movievault.data.remote.tmdb.tmdbWithSizeOrNull
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

private const val EMPTY_DEBOUNCE_MS = 300L
private const val AUTO_RETRY_THROTTLE_MS = 3_000L

private val CatalogGridSpacing = 12.dp
private val CatalogGridContentPadding =
    PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)

private val CatalogGridMaxContentWidth = 1400.dp

private val CatalogMinCardWidth = 128.dp
private val CatalogMaxCardWidth = 240.dp

private val CatalogMetaRowMinHeight = 26.dp

private val GridPosterShape =
    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

private const val COLUMNS_HARD_CAP = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    movies: LazyPagingItems<MovieRowUi>,
    gridState: LazyGridState,
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

    Box(modifier = modifier.fillMaxSize().testTag(TestTags.CATALOG_SCREEN)) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
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
                        retryButtonTestTag = TestTags.CATALOG_FULLSCREEN_RETRY,
                    )
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

                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val layout =
                            rememberCatalogGridLayout(
                                maxWidth = maxWidth,
                                maxHeight = maxHeight,
                                titleStyle = MaterialTheme.typography.titleMedium,
                            )

                        val containerPadding =
                            PaddingValues(start = layout.sideGutter, end = layout.sideGutter)
                        val posterSize = rememberGridPosterSize(layout.cellWidthDp)

                        if (!hasItemsNow) {
                            CatalogSkeletonGrid(
                                modifier = Modifier.fillMaxSize().padding(containerPadding),
                                columns = layout.columns,
                            )
                        } else {
                            CatalogGrid(
                                modifier = Modifier.fillMaxSize().padding(containerPadding),
                                columns = layout.columns,
                                movies = movies,
                                append = append,
                                gridState = gridState,
                                posterSize = posterSize,
                                onOpenDetail = onOpenDetail,
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(TestTags.CATALOG_SNACKBAR_HOST),
        ) { data ->
            MovieVaultSnackbar(data = data, modifier = Modifier.fillMaxWidth())
        }
    }
}

private data class CatalogGridLayout(val columns: Int, val sideGutter: Dp, val cellWidthDp: Dp)

@Composable
private fun rememberCatalogGridLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    titleStyle: TextStyle,
): CatalogGridLayout {
    val density = LocalDensity.current

    // limit content width on huge screens
    val contentWidth = min(maxWidth, CatalogGridMaxContentWidth)
    val sideGutter = ((maxWidth - contentWidth) / 2f).coerceAtLeast(0.dp)

    val contentWidthPx = with(density) { contentWidth.toPx() }
    val heightPx = with(density) { maxHeight.toPx() }
    val spacingPx = with(density) { CatalogGridSpacing.toPx() }
    val layoutDir = LocalLayoutDirection.current
    val hPadPx =
        with(density) {
            (CatalogGridContentPadding.calculateLeftPadding(layoutDir) +
                    CatalogGridContentPadding.calculateRightPadding(layoutDir))
                .toPx()
        }

    val minCardWidthPx = with(density) { CatalogMinCardWidth.toPx() }
    val maxCardWidthPx = with(density) { CatalogMaxCardWidth.toPx() }

    val lineHeight: TextUnit =
        if (titleStyle.lineHeight != TextUnit.Unspecified) titleStyle.lineHeight
        else (titleStyle.fontSize * 1.2f)

    val titleTwoLinesPx = with(density) { lineHeight.toPx() } * 2f

    val belowPosterContentHeightPx =
        with(density) { (12.dp * 2 + CatalogMetaRowMinHeight + 10.dp).toPx() } + titleTwoLinesPx

    val wantTwoRows = (contentWidth >= 840.dp) || (maxHeight >= 600.dp)

    val verticalGutterPx = with(density) { 16.dp.toPx() }
    val maxCellHeightPx =
        if (wantTwoRows) {
            max(0f, (heightPx - verticalGutterPx - spacingPx) / 2f)
        } else {
            max(0f, heightPx - verticalGutterPx)
        }

    val maxPosterHeightPx = max(0f, maxCellHeightPx - belowPosterContentHeightPx)
    val maxCellWidthByHeightPx =
        if (maxPosterHeightPx > 0f) (maxPosterHeightPx / 1.5f) else maxCardWidthPx

    val maxAllowedCardWidthPx = min(maxCardWidthPx, maxCellWidthByHeightPx)

    val availableWidthPx = max(0f, contentWidthPx - hPadPx)

    val minCols = if (availableWidthPx < (2f * minCardWidthPx + spacingPx)) 1 else 2

    val maxColsByMinWidth =
        floor((availableWidthPx + spacingPx) / (minCardWidthPx + spacingPx))
            .toInt()
            .coerceIn(minCols, COLUMNS_HARD_CAP)

    var chosen = maxColsByMinWidth
    for (c in minCols..maxColsByMinWidth) {
        val cellWidthPx = (availableWidthPx - spacingPx * (c - 1)) / c.toFloat()
        if (cellWidthPx <= maxAllowedCardWidthPx) {
            chosen = c
            break
        }
    }
    val cellWidthPx = (availableWidthPx - spacingPx * (chosen - 1)) / chosen.toFloat()
    val cellWidthDp = with(density) { cellWidthPx.toDp() }

    return CatalogGridLayout(columns = chosen, sideGutter = sideGutter, cellWidthDp = cellWidthDp)
}

@Composable
private fun rememberGridPosterSize(cellWidthDp: Dp): TmdbImageSize {
    val density = LocalDensity.current
    return remember(cellWidthDp, density.density) {
        val neededPx = with(density) { cellWidthDp.toPx() } * 1.15f
        when {
            neededPx <= 342f -> TmdbImageSize.List
            neededPx <= 500f -> TmdbImageSize.Grid
            else -> TmdbImageSize.GridLarge
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
private fun CatalogSkeletonGrid(modifier: Modifier = Modifier, columns: Int, count: Int = 12) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = CatalogGridContentPadding,
        horizontalArrangement = Arrangement.spacedBy(CatalogGridSpacing),
        verticalArrangement = Arrangement.spacedBy(CatalogGridSpacing),
    ) {
        items(count, key = { "skeleton_$it" }) { MovieGridCellPlaceholder() }

        item(key = "skeleton_footer", span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CatalogGrid(
    columns: Int,
    movies: LazyPagingItems<MovieRowUi>,
    append: LoadState,
    gridState: LazyGridState,
    posterSize: TmdbImageSize,
    onOpenDetail: (movieId: Long, title: String) -> Unit,
    modifier: Modifier = Modifier,
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize().testTag(TestTags.CATALOG_GRID),
        contentPadding = CatalogGridContentPadding,
        horizontalArrangement = Arrangement.spacedBy(CatalogGridSpacing),
        verticalArrangement = Arrangement.spacedBy(CatalogGridSpacing),
    ) {
        items(
            count = movies.itemCount,
            key = movies.itemKey { it.id },
            contentType = movies.itemContentType { "movie" },
        ) { index ->
            val row = movies[index]
            if (row == null) {
                MovieGridCellPlaceholder()
            } else {
                MovieGridCell(
                    title = row.title,
                    releaseYear = row.releaseYear,
                    rating = row.rating,
                    posterUrl = row.posterUrl.tmdbWithSizeOrNull(posterSize),
                    onClick = { onOpenDetail(row.id, row.title) },
                    modifier = Modifier.testTag(TestTags.CATALOG_ITEM + row.id),
                )
            }
        }

        // span all columns
        item(key = "append_footer", contentType = "footer", span = { GridItemSpan(maxLineSpan) }) {
            AppendFooter(
                append = append,
                stickyError = stickyAppendError,
                onRetry = { movies.retry() },
            )
        }
    }

    AutoRetryAppendOnScroll(movies = movies, append = append, gridState = gridState)
}

@Composable
private fun MovieGridCell(
    title: String,
    releaseYear: Int?,
    rating: Double?,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MovieVaultCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MoviePoster(
                posterUrl = posterUrl,
                contentDescription = "$title poster",
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                shape = GridPosterShape,
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val hasBoth = rating != null && releaseYear != null
                val yearText = releaseYear?.toString()

                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = CatalogMetaRowMinHeight),
                    horizontalArrangement =
                        if (hasBoth) Arrangement.SpaceBetween else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        rating != null -> RatingPill(rating = rating)
                        yearText != null -> MetaPill(text = yearText)
                    }
                    if (hasBoth) MetaPill(text = yearText!!)
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MovieGridCellPlaceholder(modifier: Modifier = Modifier) {
    MovieVaultCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(
                modifier =
                    Modifier.fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(GridPosterShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = CatalogMetaRowMinHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PillSkeleton(width = 56.dp)
                    PillSkeleton(width = 56.dp)
                }
                val titleStyle = MaterialTheme.typography.titleMedium
                val titleHeight = rememberTwoLineTitleHeightDp(titleStyle)
                Column(
                    modifier = Modifier.fillMaxWidth().height(titleHeight),
                    verticalArrangement = Arrangement.SpaceAround,
                ) {
                    SkeletonLine(widthFraction = 0.88f, height = 18.dp)
                    SkeletonLine(widthFraction = 0.70f, height = 18.dp)
                }
            }
        }
    }
}

@Composable
private fun rememberTwoLineTitleHeightDp(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val result =
        remember(style) {
            measurer.measure(text = AnnotatedString("A\nA"), style = style, maxLines = 2)
        }

    return with(density) { result.size.height.toDp() }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: Dp) {
    Spacer(
        modifier =
            Modifier.fillMaxWidth(widthFraction)
                .height(height)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun PillSkeleton(width: Dp, modifier: Modifier = Modifier) {
    Spacer(
        modifier =
            modifier
                .height(CatalogMetaRowMinHeight)
                .width(width)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
    )
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
    gridState: LazyGridState,
) {
    var lastAutoRetryAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(movies, gridState, append) {
        snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val isScrolling = gridState.isScrollInProgress
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
                modifier = Modifier.weight(1f).testTag(TestTags.CATALOG_SNACKBAR_MESSAGE),
            )

            data.visuals.actionLabel?.let { label ->
                TextButton(
                    onClick = { data.performAction() },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.inversePrimary
                        ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag(TestTags.CATALOG_SNACKBAR_ACTION),
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
