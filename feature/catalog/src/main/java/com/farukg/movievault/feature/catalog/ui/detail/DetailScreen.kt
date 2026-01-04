package com.farukg.movievault.feature.catalog.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.core.ui.ErrorState
import com.farukg.movievault.core.ui.components.MetaPill
import com.farukg.movievault.core.ui.components.MoviePoster
import com.farukg.movievault.core.ui.components.MovieVaultCard
import com.farukg.movievault.core.ui.components.RatingPill
import com.farukg.movievault.core.ui.components.TagPill
import com.farukg.movievault.core.ui.testing.TestTags
import com.farukg.movievault.feature.catalog.ui.components.detailPosterHeightDp

private const val DETAIL_SKELETON_SHOW_DELAY_MS = 180L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    titleHint: String?,
    onRefresh: () -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()

    val refreshing =
        when (uiState) {
            is DetailUiState.Content -> uiState.isRefreshing
            is DetailUiState.Loading -> uiState.isRefreshing
            else -> false
        }

    val pullEnabled =
        when (uiState) {
            is DetailUiState.Content -> !uiState.hasFetchedDetail
            is DetailUiState.Loading -> true
            is DetailUiState.NoCacheError -> true
        }
    val showFullSkeleton =
        rememberDelayedTrue(
            target = uiState is DetailUiState.Loading,
            delayMs = DETAIL_SKELETON_SHOW_DELAY_MS,
        )
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(TestTags.DETAIL_SCREEN)
                .padding(horizontal = 16.dp)
                .pullToRefresh(
                    isRefreshing = refreshing,
                    state = pullState,
                    enabled = pullEnabled,
                    onRefresh = { if (!refreshing) onRefresh() },
                )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val bodyModifier = Modifier.weight(1f).fillMaxWidth()

            when (uiState) {
                is DetailUiState.Loading -> {
                    if (showFullSkeleton) {
                        DetailSkeleton(modifier = bodyModifier, titleHint = titleHint)
                    } else {
                        Box(modifier = bodyModifier)
                    }
                }

                is DetailUiState.NoCacheError -> {
                    ErrorState(
                        modifier = bodyModifier,
                        message = uiState.error.userMessage(),
                        onRetry = onRefresh,
                    )
                }

                is DetailUiState.Content -> {
                    val bannerError = uiState.bannerError

                    val bannerState = remember { MutableTransitionState(false) }
                    bannerState.targetState = bannerError != null

                    var lastBannerError by remember { mutableStateOf<AppError?>(null) }
                    if (bannerError != null) lastBannerError = bannerError
                    if (!bannerState.currentState && !bannerState.targetState)
                        lastBannerError = null

                    val showPlaceholders =
                        !uiState.hasFetchedDetail &&
                            (uiState.isRefreshing || !uiState.hasAttemptedRefresh)

                    Column(modifier = bodyModifier.verticalScroll(scrollState)) {
                        AnimatedVisibility(
                            visibleState = bannerState,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        ) {
                            val err = bannerError ?: lastBannerError ?: return@AnimatedVisibility
                            Column {
                                DetailErrorBanner(
                                    error = err,
                                    isRefreshing = uiState.isRefreshing,
                                    onRetry = onRefresh,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        MovieVaultCard(modifier = Modifier.fillMaxWidth()) {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                val posterHeight = detailPosterHeightDp()
                                val posterWidth = posterHeight * (2f / 3f)

                                val spacer = 16.dp
                                val minRightColumnWidth = 150.dp
                                val availableRight = maxWidth - posterWidth - spacer
                                val compact = availableRight < minRightColumnWidth

                                if (compact) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        MoviePoster(
                                            posterUrl = uiState.posterUrl,
                                            fallbackPosterUrl = uiState.posterFallbackUrl,
                                            contentDescription = "${uiState.title} poster",
                                            modifier =
                                                Modifier.height(posterHeight).aspectRatio(2f / 3f),
                                        )

                                        DetailHeaderMeta(
                                            uiState = uiState,
                                            showPlaceholders = showPlaceholders,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        MoviePoster(
                                            posterUrl = uiState.posterUrl,
                                            fallbackPosterUrl = uiState.posterFallbackUrl,
                                            contentDescription = "${uiState.title} poster",
                                            modifier =
                                                Modifier.height(posterHeight).aspectRatio(2f / 3f),
                                        )

                                        Spacer(modifier = Modifier.width(spacer))
                                        DetailHeaderMeta(
                                            uiState = uiState,
                                            showPlaceholders = showPlaceholders,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (uiState.overview.isNotBlank()) {
                            MovieVaultCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = uiState.overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier.padding(16.dp).testTag(TestTags.DETAIL_OVERVIEW),
                                )
                            }
                        } else if (showPlaceholders) {
                            OverviewSkeletonCard(modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
        PullToRefreshDefaults.Indicator(
            state = pullState,
            isRefreshing = refreshing,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun DetailErrorBanner(
    error: AppError,
    isRefreshing: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offline = error is AppError.Offline
    val icon = if (offline) Icons.Outlined.CloudOff else Icons.Outlined.SyncProblem
    val message = if (offline) "Offline — showing saved details" else "Couldn't update details"

    MovieVaultCard(modifier = modifier.testTag(TestTags.DETAIL_ERROR_BANNER)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (offline) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            TextButton(
                onClick = onRetry,
                enabled = !isRefreshing,
                modifier = Modifier.testTag(TestTags.DETAIL_ERROR_RETRY),
            ) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeaderMeta(
    uiState: DetailUiState.Content,
    showPlaceholders: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(TestTags.DETAIL_TITLE),
        )

        if (uiState.genres.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().testTag(TestTags.DETAIL_GENRES),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.genres.take(6).forEach { genre -> TagPill(text = genre) }
            }
        } else if (showPlaceholders) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) {
                    MetaPillSkeleton(
                        width =
                            when (it) {
                                0 -> 68.dp
                                1 -> 54.dp
                                2 -> 76.dp
                                else -> 60.dp
                            }
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.releaseYear?.let { MetaPill(text = it.toString()) }

            when {
                uiState.runtimeMinutes != null ->
                    MetaPill(
                        text = "${uiState.runtimeMinutes}m",
                        modifier = Modifier.testTag(TestTags.DETAIL_RUNTIME),
                    )
                showPlaceholders -> MetaPillSkeleton(width = 54.dp)
            }

            uiState.rating?.let { RatingPill(rating = it) }
        }
    }
}

@Composable
private fun rememberDelayedTrue(target: Boolean, delayMs: Long): Boolean {
    var value by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (!target) {
            value = false
        } else {
            kotlinx.coroutines.delay(delayMs)
            value = true
        }
    }
    return value
}

// ------------- Skeletons -------------

@Composable
private fun DetailSkeleton(modifier: Modifier, titleHint: String?) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MovieVaultCard(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                val posterHeight = detailPosterHeightDp()
                val posterWidth = posterHeight * (2f / 3f)

                val spacer = 16.dp
                val minRightColumnWidth = 150.dp
                val availableRight = maxWidth - posterWidth - spacer
                val compact = availableRight < minRightColumnWidth

                if (compact) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PosterSkeleton(height = posterHeight)
                        HeaderTextSkeleton(titleHint = titleHint)
                        HeaderMetaSkeleton()
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        PosterSkeleton(height = posterHeight)
                        Spacer(Modifier.width(spacer))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HeaderTextSkeleton(titleHint = titleHint)
                            HeaderMetaSkeleton()
                        }
                    }
                }
            }
        }

        OverviewSkeletonCard(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PosterSkeleton(height: Dp) {
    Spacer(
        modifier =
            Modifier.height(height)
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun HeaderTextSkeleton(titleHint: String?) {
    if (!titleHint.isNullOrBlank()) {
        Text(
            text = titleHint,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        SkeletonLine(widthFraction = 0.72f, height = 22.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonLine(widthFraction = 0.55f, height = 18.dp)
    }
}

@Composable
private fun HeaderMetaSkeleton() {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(4) {
            MetaPillSkeleton(
                width =
                    when (it) {
                        0 -> 68.dp
                        1 -> 54.dp
                        2 -> 76.dp
                        else -> 60.dp
                    }
            )
        }
        MetaPillSkeleton(width = 52.dp)
        MetaPillSkeleton(width = 64.dp)
    }
}

@Composable
private fun OverviewSkeletonCard(modifier: Modifier = Modifier) {
    MovieVaultCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(5) { idx ->
                SkeletonLine(
                    widthFraction =
                        when (idx) {
                            0 -> 0.92f
                            1 -> 0.86f
                            2 -> 0.90f
                            3 -> 0.78f
                            else -> 0.60f
                        },
                    height = 14.dp,
                )
            }
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: Dp) {
    Spacer(
        modifier =
            Modifier.fillMaxWidth(widthFraction)
                .height(height)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun MetaPillSkeleton(width: Dp, modifier: Modifier = Modifier) {
    Spacer(
        modifier =
            modifier
                .height(26.dp)
                .width(width)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
