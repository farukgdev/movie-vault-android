package com.farukg.movievault.feature.catalog.ui.catalog.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farukg.movievault.feature.catalog.ui.catalog.CatalogStatusIcon
import com.farukg.movievault.feature.catalog.ui.catalog.CatalogStatusUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTopAppBar(
    statusUi: CatalogStatusUi,
    onOpenStatus: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = "MovieVault", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            StatusActionButton(statusUi = statusUi, onClick = onOpenStatus)
            IconButton(onClick = onOpenFavorites) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorites")
            }
        },
    )
}

@Composable
private fun StatusActionButton(
    statusUi: CatalogStatusUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentation = statusUi.toPresentation()

    FilledTonalIconButton(
        onClick = onClick,
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        modifier = modifier.semantics { contentDescription = presentation.contentDescription },
    ) {
        when (statusUi.icon) {
            CatalogStatusIcon.BackgroundRefreshing -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CatalogStatusIcon.Stale ->
                Icon(imageVector = Icons.Outlined.Sync, contentDescription = null)
            CatalogStatusIcon.Offline ->
                Icon(imageVector = Icons.Outlined.CloudOff, contentDescription = null)
            CatalogStatusIcon.Error ->
                Icon(
                    imageVector = Icons.Outlined.SyncProblem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            CatalogStatusIcon.Ok ->
                Icon(imageVector = Icons.Outlined.CloudDone, contentDescription = null)
        }
    }
}
