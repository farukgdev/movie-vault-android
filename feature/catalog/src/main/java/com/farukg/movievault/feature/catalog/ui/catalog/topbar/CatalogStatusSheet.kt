package com.farukg.movievault.feature.catalog.ui.catalog.topbar

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.farukg.movievault.core.error.userMessage
import com.farukg.movievault.feature.catalog.ui.catalog.CatalogStatusIcon
import com.farukg.movievault.feature.catalog.ui.catalog.CatalogStatusUi
import com.farukg.movievault.feature.catalog.ui.catalog.RefreshOrigin
import kotlin.math.max
import kotlinx.coroutines.delay

@Composable
fun CatalogStatusSheetContent(
    statusUi: CatalogStatusUi,
    onRefreshNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val hasProblem = statusUi.error != null
        val presentation = statusUi.toPresentation()

        Text(text = presentation.headline, style = MaterialTheme.typography.titleLarge)

        if (statusUi.icon == CatalogStatusIcon.Stale && statusUi.error == null) {
            Text(
                text = "Catalog may be out of date. Refresh to get the latest popular movies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        statusUi.error?.let { err ->
            Text(
                text = err.userMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = rememberRelativeUpdatedTextSmart(statusUi.lastUpdatedEpochMillis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val primaryActionLabel = if (hasProblem) "Try again" else "Refresh now"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text("Close") }
            TextButton(onClick = onRefreshNow) { Text(primaryActionLabel) }
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun rememberRelativeUpdatedTextSmart(lastUpdatedEpochMillis: Long?): String {
    if (lastUpdatedEpochMillis == null) return "Last updated: Never"

    val now by
        produceState(initialValue = System.currentTimeMillis(), key1 = lastUpdatedEpochMillis) {
            while (true) {
                val current = System.currentTimeMillis()
                value = current

                val ageMs = current - lastUpdatedEpochMillis

                val delayMs =
                    if (ageMs < 60_000L) {
                        60_000L - max(ageMs, 0L)
                    } else {
                        60_000L - (current % 60_000L)
                    }

                delay(delayMs.coerceAtLeast(1L))
            }
        }

    val ageMs = now - lastUpdatedEpochMillis
    return if (ageMs < 60_000L) {
        "Updated just now"
    } else {
        "Last updated: " +
            DateUtils.getRelativeTimeSpanString(
                lastUpdatedEpochMillis,
                now,
                DateUtils.MINUTE_IN_MILLIS,
            )
    }
}

internal data class StatusPresentation(val contentDescription: String, val headline: String)

internal fun CatalogStatusUi.toPresentation(): StatusPresentation {
    val contentDescription =
        when (icon) {
            CatalogStatusIcon.BackgroundRefreshing -> "Status: refreshing"
            CatalogStatusIcon.Stale -> "Status: out of date"
            CatalogStatusIcon.Offline -> "Status: offline"
            CatalogStatusIcon.Error -> "Status: issue"
            CatalogStatusIcon.Ok -> "Status: up to date"
        }

    val headline =
        when {
            isRefreshing -> "Refreshing..."
            icon == CatalogStatusIcon.Stale -> "Out of date"
            icon == CatalogStatusIcon.Offline -> "Offline"
            icon == CatalogStatusIcon.Error -> {
                when (errorOrigin) {
                    RefreshOrigin.Automatic -> "Background refresh failed"
                    RefreshOrigin.Manual -> "Your refresh failed"
                    null -> "Refresh failed"
                }
            }
            else -> "Up to date"
        }

    return StatusPresentation(contentDescription = contentDescription, headline = headline)
}
