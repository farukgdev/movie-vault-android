package com.farukg.movievault.feature.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

@Composable
fun MoviePoster(
    posterUrl: String?,
    fallbackPosterUrl: String? = null,
    contentDescription: String?,
    modifier: Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val primary = posterUrl?.trim().takeUnless { it.isNullOrBlank() }
    val fallback = fallbackPosterUrl?.trim().takeUnless { it.isNullOrBlank() }

    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    var activeUrl by remember(primary, fallback) { mutableStateOf(primary) }
    var isError by remember(primary, fallback) { mutableStateOf(false) }

    Box(
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (activeUrl != null) {
            AsyncImage(
                model = activeUrl,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) {
                        val next = nextPosterUrlOnError(activeUrl, primary, fallback)
                        if (next != activeUrl) {
                            activeUrl = next
                            isError = false
                        } else {
                            isError = true
                        }
                    } else {
                        isError = false
                    }
                },
            )
        }

        if (activeUrl != null && isError) {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

internal fun nextPosterUrlOnError(
    activeUrl: String?,
    primaryUrl: String?,
    fallbackUrl: String?,
): String? {
    return if (activeUrl == primaryUrl && !fallbackUrl.isNullOrBlank()) fallbackUrl else activeUrl
}
