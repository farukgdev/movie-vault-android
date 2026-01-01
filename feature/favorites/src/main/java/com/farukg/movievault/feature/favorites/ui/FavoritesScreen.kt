package com.farukg.movievault.feature.favorites.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.core.ui.EmptyState
import com.farukg.movievault.core.ui.ErrorState
import com.farukg.movievault.core.ui.LoadingState
import com.farukg.movievault.core.ui.components.MetaPill
import com.farukg.movievault.core.ui.components.MoviePoster
import com.farukg.movievault.core.ui.components.MovieVaultCard
import com.farukg.movievault.core.ui.components.RatingPill

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedCount =
        when (uiState) {
            is FavoritesUiState.Content -> uiState.movies.size
            else -> 0
        }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FavoritesTopBar(onBack = onBack, savedCount = savedCount)

        val bodyModifier = Modifier.weight(1f).fillMaxWidth()

        when (uiState) {
            FavoritesUiState.Loading ->
                LoadingState(modifier = bodyModifier, message = "Loading favorites...")

            is FavoritesUiState.Error ->
                ErrorState(
                    modifier = bodyModifier,
                    message = uiState.error.userMessage(),
                    onRetry = onRetry,
                )

            FavoritesUiState.Empty ->
                EmptyState(
                    modifier = bodyModifier,
                    title = "No favorites yet",
                    message = "Tap the heart on a movie to save it here.",
                )

            is FavoritesUiState.Content -> {
                LazyColumn(
                    modifier = bodyModifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(start = 0.dp, top = 6.dp, end = 0.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.movies, key = { it.id }) { movie ->
                        FavoriteRow(
                            movie = movie,
                            onOpenDetail = { onOpenDetail(movie.id) },
                            onUnfavorite = { onToggleFavorite(movie.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesTopBar(onBack: () -> Unit, savedCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }

        Text(
            text = "Favorites",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = "$savedCount saved",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteRow(
    movie: FavoriteRowUi,
    onOpenDetail: () -> Unit,
    onUnfavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MovieVaultCard(onClick = onOpenDetail, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val posterHeight = 64.dp

            MoviePoster(
                posterUrl = movie.posterUrl,
                contentDescription = "${movie.title} poster",
                modifier = Modifier.height(posterHeight).aspectRatio(2f / 3f),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (movie.releaseYear != null || movie.rating != null) {
                    Spacer(Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        movie.releaseYear?.let { MetaPill(text = it.toString()) }
                        movie.rating?.let { RatingPill(rating = it) }
                    }
                }
            }

            IconButton(
                onClick = onUnfavorite,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
