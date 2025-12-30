package com.farukg.movievault.feature.catalog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun catalogPosterHeightDp(): Dp {
    val w = LocalWindowInfo.current.containerSize.width
    return when {
        w >= 840 -> 88.dp
        w >= 600 -> 80.dp
        else -> 72.dp
    }
}

@Composable
fun detailPosterHeightDp(): Dp {
    val w = LocalWindowInfo.current.containerSize.width
    return when {
        w >= 840 -> 200.dp
        w >= 600 -> 170.dp
        else -> 140.dp
    }
}
