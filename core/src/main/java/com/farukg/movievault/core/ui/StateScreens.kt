package com.farukg.movievault.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.ui.testing.TestTags

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    message: String? = null,
    retryLabel: String = "Retry",
    onRetry: (() -> Unit)? = null,
    retryButtonTestTag: String? = null,
) {
    Box(
        modifier = modifier.fillMaxSize().testTag(TestTags.FULLSCREEN_ERROR),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(imageVector = Icons.Outlined.ErrorOutline, contentDescription = null)
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (!message.isNullOrBlank()) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    modifier =
                        if (retryButtonTestTag != null) Modifier.testTag(retryButtonTestTag)
                        else Modifier,
                ) {
                    Text(text = retryLabel)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize().testTag(TestTags.EMPTY_STATE),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Inbox, contentDescription = null)
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (!message.isNullOrBlank()) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Button(onClick = onAction) { Text(text = actionLabel) }
            }
        }
    }
}
