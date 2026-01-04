package com.farukg.movievault.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

private object UiWait {
    private const val DEFAULT_TIMEOUT_MS = 15_000L

    fun timeoutMs(override: Long?): Long {
        if (override != null) return override
        val fromArgs =
            InstrumentationRegistry.getArguments().getString("uiTimeoutMs")?.toLongOrNull()
        return fromArgs ?: DEFAULT_TIMEOUT_MS
    }
}

fun ComposeTestRule.waitUntilExists(matcher: SemanticsMatcher, timeoutMs: Long? = null) {
    waitUntil(UiWait.timeoutMs(timeoutMs)) {
        onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeTestRule.waitUntilGone(matcher: SemanticsMatcher, timeoutMs: Long? = null) {
    waitUntil(UiWait.timeoutMs(timeoutMs)) { onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }
}

fun ComposeTestRule.waitUntilTagExists(tag: String, timeoutMs: Long? = null) {
    waitUntilExists(hasTestTag(tag), timeoutMs)
}

fun ComposeTestRule.waitUntilTagGone(tag: String, timeoutMs: Long? = null) {
    waitUntilGone(hasTestTag(tag), timeoutMs)
}
