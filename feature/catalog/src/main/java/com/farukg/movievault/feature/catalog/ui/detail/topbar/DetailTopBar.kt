package com.farukg.movievault.feature.catalog.ui.detail.topbar

import android.R.attr.contentDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.farukg.movievault.core.ui.testing.TestTags
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
            IconButton(onClick = onBack, modifier = Modifier.testTag(TestTags.DETAIL_BACK_BUTTON)) {
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
            IconButton(
                onClick = onToggleFavorite,
                enabled = canToggle,
                modifier =
                    Modifier.testTag(TestTags.DETAIL_FAVORITE_BUTTON).semantics {
                        contentDescription = if (isFav) "Unfavorite" else "Favorite"
                    },
            ) {
                Icon(
                    imageVector =
                        if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                )
            }
        },
    )
}
