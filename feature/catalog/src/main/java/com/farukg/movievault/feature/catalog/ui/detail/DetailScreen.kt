package com.farukg.movievault.feature.catalog.ui.detail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.core.ui.ErrorState
import com.farukg.movievault.core.ui.LoadingState
import com.farukg.movievault.core.ui.components.MetaPill
import com.farukg.movievault.core.ui.components.MoviePoster
import com.farukg.movievault.core.ui.components.MovieVaultCard
import com.farukg.movievault.core.ui.components.RatingPill
import com.farukg.movievault.core.ui.components.TagPill
import com.farukg.movievault.feature.catalog.ui.components.detailPosterHeightDp

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onRetry: () -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val bodyModifier = Modifier.weight(1f).fillMaxWidth()

        when (uiState) {
            DetailUiState.Loading ->
                LoadingState(modifier = bodyModifier, message = "Loading details...")

            is DetailUiState.Error ->
                ErrorState(
                    modifier = bodyModifier,
                    message = uiState.error.userMessage(),
                    onRetry = onRetry,
                )

            is DetailUiState.Content -> {
                Column(
                    modifier = bodyModifier.verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    MovieVaultCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = uiState.overview.ifBlank { "No overview available." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeaderMeta(uiState: DetailUiState.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        if (uiState.genres.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.genres.take(6).forEach { genre -> TagPill(text = genre) }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.releaseYear?.let { MetaPill(text = it.toString()) }
            uiState.runtimeMinutes?.let { MetaPill(text = "${it}m") }
            uiState.rating?.let { RatingPill(rating = it) }
        }
    }
}
