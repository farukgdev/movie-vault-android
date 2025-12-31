package com.farukg.movievault.feature.catalog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun windowWidthDp(): Dp {
    val px = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current
    return with(density) { px.toDp() }
}

@Composable
fun catalogPosterHeightDp(): Dp {
    val w = windowWidthDp()
    return when {
        w >= 840.dp -> 88.dp
        w >= 600.dp -> 80.dp
        else -> 72.dp
    }
}

@Composable
fun detailPosterHeightDp(): Dp {
    val w = windowWidthDp()
    return when {
        w >= 840.dp -> 200.dp
        w >= 600.dp -> 170.dp
        else -> 140.dp
    }
}
