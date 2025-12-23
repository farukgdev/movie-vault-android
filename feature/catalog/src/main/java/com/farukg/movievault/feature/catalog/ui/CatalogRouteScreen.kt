package com.farukg.movievault.feature.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun CatalogRouteScreen(
    onOpenDetail: (movieId: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sampleMovies = remember {
        listOf(
            MovieRowUi(id = 123L, title = "Dune: Part Two", subtitle = "Sci-Fi • 2024"),
            MovieRowUi(id = 456L, title = "Oppenheimer", subtitle = "Drama • 2023"),
            MovieRowUi(
                id = 789L,
                title = "Spider-Man: Across the Spider-Verse",
                subtitle = "Animation • 2023",
            ),
        )
    }

    CatalogScreen(
        uiState = CatalogUiState.Content(sampleMovies),
        onRetry = {},
        onOpenDetail = onOpenDetail,
        onOpenFavorites = onOpenFavorites,
        modifier = modifier,
    )
}
