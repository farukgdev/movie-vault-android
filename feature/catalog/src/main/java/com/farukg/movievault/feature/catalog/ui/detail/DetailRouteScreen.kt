package com.farukg.movievault.feature.catalog.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DetailRouteScreen(movieId: Long, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: DetailViewModel = hiltViewModel()

    LaunchedEffect(movieId) { viewModel.setMovieId(movieId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onBack = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        modifier = modifier,
    )
}
