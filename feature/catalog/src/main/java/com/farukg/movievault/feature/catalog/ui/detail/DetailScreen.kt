package com.farukg.movievault.feature.catalog.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.core.ui.ErrorState
import com.farukg.movievault.core.ui.LoadingState
import com.farukg.movievault.core.ui.components.MovieVaultCard
import com.farukg.movievault.feature.catalog.ui.components.MoviePoster
import com.farukg.movievault.feature.catalog.ui.components.detailPosterHeightDp

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val bodyModifier = Modifier.weight(1f).fillMaxWidth()

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }

            Text(
                text = "Details",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = onToggleFavorite) {
                val isFav = (uiState as? DetailUiState.Content)?.isFavorite == true
                Icon(
                    imageVector =
                        if (isFav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFav) "Unfavorite" else "Favorite",
                )
            }
        }

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
                Column(modifier = bodyModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MovieVaultCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            val posterHeight = detailPosterHeightDp()
                            MoviePoster(
                                posterUrl = uiState.posterUrl,
                                fallbackPosterUrl = uiState.posterFallbackUrl,
                                contentDescription = "${uiState.title} poster",
                                modifier = Modifier.height(posterHeight).aspectRatio(2f / 3f),
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.title,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (uiState.metaPrimary.isNotBlank()) {
                                    Text(
                                        text = uiState.metaPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (uiState.metaSecondary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.metaSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                }
            }
        }
    }
}
