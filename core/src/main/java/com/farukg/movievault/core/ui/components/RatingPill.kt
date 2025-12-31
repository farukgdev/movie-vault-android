package com.farukg.movievault.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.Locale

@Composable
fun RatingPill(rating: Double, modifier: Modifier = Modifier) {
    MetaPill(
        text = String.format(Locale.US, "%.1f", rating),
        leadingIcon = Icons.Outlined.Star,
        contentDescription = "Rating",
        modifier = modifier,
    )
}
