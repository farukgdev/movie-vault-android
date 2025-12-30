package com.farukg.movievault.feature.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

@Composable
fun MoviePoster(
    posterUrl: String?,
    contentDescription: String?,
    modifier: Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val transparentPainter = remember { ColorPainter(Color.Transparent) }

    var isError by remember(posterUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                placeholder = transparentPainter,
                error = transparentPainter,
                fallback = transparentPainter,
                onLoading = { isError = false },
                onSuccess = { isError = false },
                onError = { _: AsyncImagePainter.State.Error -> isError = true },
            )
        }

        if (posterUrl != null && isError) {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
