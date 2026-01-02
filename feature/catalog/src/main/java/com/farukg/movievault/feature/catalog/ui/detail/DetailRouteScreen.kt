package com.farukg.movievault.feature.catalog.ui.detail

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farukg.movievault.core.ui.scaffold.RegisterTopBar
import com.farukg.movievault.feature.catalog.navigation.DetailRoute
import com.farukg.movievault.feature.catalog.ui.detail.topbar.DetailTopAppBar

@Composable
fun DetailRouteScreen(route: DetailRoute, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: DetailViewModel = hiltViewModel()
    LaunchedEffect(route.movieId) { viewModel.setMovieId(route.movieId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    val thresholdPx = with(LocalDensity.current) { 40.dp.roundToPx() }
    val showBarTitle by remember { derivedStateOf { scrollState.value > thresholdPx } }

    RegisterTopBar(route) {
        DetailTopAppBar(
            uiState = uiState,
            initialTitle = route.initialTitle,
            showTitle = showBarTitle,
            onBack = onBack,
            onToggleFavorite = viewModel::toggleFavorite,
        )
    }

    DetailScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        scrollState = scrollState,
        modifier = modifier,
    )
}
