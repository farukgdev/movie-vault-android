package com.farukg.movievault.feature.catalog.ui.detail.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.farukg.movievault.feature.catalog.ui.detail.DetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopAppBar(
    uiState: DetailUiState,
    initialTitle: String?,
    showTitle: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val content = uiState as? DetailUiState.Content
    val title = content?.title ?: initialTitle ?: "Details"

    val isFav = content?.isFavorite == true
    val canToggle = content != null

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        title = {
            if (showTitle) {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            IconButton(onClick = onToggleFavorite, enabled = canToggle) {
                Icon(
                    imageVector =
                        if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFav) "Unfavorite" else "Favorite",
                )
            }
        },
    )
}
