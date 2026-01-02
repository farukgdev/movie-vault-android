package com.farukg.movievault.feature.favorites.ui.topbar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.ui.components.MetaPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesTopAppBar(onBack: () -> Unit, savedCount: Int?) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        title = { Text(text = "Favorites", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            val count = savedCount ?: return@TopAppBar
            if (count > 0) {
                MetaPill(
                    text = "Saved $count",
                    modifier = Modifier.padding(end = 8.dp),
                    leadingIcon = null,
                    contentDescription = "Saved count",
                )
            }
        },
    )
}
