package com.farukg.movievault.feature.catalog.ui.catalog

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.core.ui.EmptyState
import com.farukg.movievault.core.ui.ErrorState
import com.farukg.movievault.core.ui.LoadingState
import com.farukg.movievault.data.repository.CatalogRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    uiState: CatalogUiState,
    refreshState: CatalogRefreshState,
    refreshEvents: Flow<AppError>,
    isManualRefreshing: Boolean,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (movieId: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isRefreshingUi = rememberMinManualRefreshIndicatorDuration(isManualRefreshing)

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
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBar(
                lastUpdatedEpochMillis = refreshState.lastUpdatedEpochMillis,
                onOpenFavorites = onOpenFavorites,
            )

            val bodyModifier = Modifier.weight(1f).fillMaxWidth()

            PullToRefreshBox(
                modifier = bodyModifier,
                isRefreshing = isRefreshingUi,
                onRefresh = { if (!isManualRefreshing) onRefresh() },
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = isRefreshingUi,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (uiState) {
                        CatalogUiState.Loading -> {
                            item {
                                FullScreenCentered { LoadingState(message = "Loading catalog...") }
                            }
                        }

                        is CatalogUiState.Error -> {
                            item {
                                FullScreenCentered {
                                    ErrorState(
                                        message = uiState.error.userMessage(),
                                        onRetry = onRetry,
                                    )
                                }
                            }
                        }

                        CatalogUiState.Empty -> {
                            val hasNeverUpdated = refreshState.lastUpdatedEpochMillis == null
                            val initialFailure =
                                hasNeverUpdated && refreshState.lastRefreshError != null
                            val initialLoading =
                                hasNeverUpdated && refreshState.lastRefreshError == null

                            item {
                                FullScreenCentered {
                                    when {
                                        initialLoading ->
                                            LoadingState(message = "Loading catalog...")

                                        initialFailure -> {
                                            val err = refreshState.lastRefreshError
                                            ErrorState(
                                                message =
                                                    err?.userMessage() ?: "Something went wrong.",
                                                onRetry = onRetry,
                                            )
                                        }

                                        else ->
                                            EmptyState(
                                                title = "No movies yet",
                                                message = "Pull down to refresh.",
                                                actionLabel = "Try again",
                                                onAction = onRefresh,
                                            )
                                    }
                                }
                            }
                        }

                        is CatalogUiState.Content -> {
                            items(uiState.movies, key = { it.id }) { movie ->
                                MovieRow(
                                    title = movie.title,
                                    subtitle = movie.subtitle,
                                    onClick = { onOpenDetail(movie.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) { data ->
            MovieVaultSnackbar(data = data, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TopBar(
    lastUpdatedEpochMillis: Long?,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "MovieVault", style = MaterialTheme.typography.headlineSmall)

            if (lastUpdatedEpochMillis != null) {
                val updatedText = rememberRelativeUpdatedText(lastUpdatedEpochMillis)
                Text(
                    text = updatedText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(onClick = onOpenFavorites) {
            Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = "Favorites")
        }
    }
}

@Composable
private fun rememberRelativeUpdatedText(lastUpdatedEpochMillis: Long): String {
    var now by remember(lastUpdatedEpochMillis) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastUpdatedEpochMillis) {
        while (true) {
            val current = System.currentTimeMillis()
            val age = current - lastUpdatedEpochMillis
            now = current

            val delayMs = if (age < 60_000L) 1_000L else 60_000L - (current % 60_000L)
            delay(delayMs)
        }
    }

    val age = now - lastUpdatedEpochMillis
    return if (age < 60_000L) {
        "Updated just now"
    } else {
        "Updated " +
            DateUtils.getRelativeTimeSpanString(
                lastUpdatedEpochMillis,
                now,
                DateUtils.MINUTE_IN_MILLIS,
            )
    }
}

@Composable
private fun LazyItemScope.FullScreenCentered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillParentMaxSize().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun MovieRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier =
                    Modifier.size(width = 52.dp, height = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        is AppError.Unknown -> "Couldn’t refresh"
    }
