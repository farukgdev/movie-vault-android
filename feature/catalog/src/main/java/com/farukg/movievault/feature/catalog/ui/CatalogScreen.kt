package com.farukg.movievault.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.core.ui.EmptyState
import com.farukg.movievault.core.ui.ErrorState
import com.farukg.movievault.core.ui.LoadingState

@Composable
fun CatalogScreen(
    uiState: CatalogUiState,
    onRetry: () -> Unit,
    onOpenDetail: (movieId: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val bodyModifier = Modifier.weight(1f).fillMaxWidth()

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MovieVault",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = onOpenFavorites) {
                Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = "Favorites")
            }
        }

        when (uiState) {
            CatalogUiState.Loading ->
                LoadingState(modifier = bodyModifier, message = "Loading catalog...")

            is CatalogUiState.Error ->
                ErrorState(
                    modifier = bodyModifier,
                    message = uiState.error.userMessage(),
                    onRetry = onRetry,
                )

            CatalogUiState.Empty ->
                EmptyState(
                    modifier = bodyModifier,
                    title = "No movies found",
                    message = "Try refreshing or check your connection.",
                )

            is CatalogUiState.Content -> {
                LazyColumn(
                    modifier = bodyModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier =
                    Modifier.size(width = 56.dp, height = 72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
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
