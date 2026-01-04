package com.farukg.movievault.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule

fun ComposeTestRule.waitUntilExists(matcher: SemanticsMatcher, timeoutMs: Long = 5_000L) {
    waitUntil(timeoutMs) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
}

fun ComposeTestRule.waitUntilGone(matcher: SemanticsMatcher, timeoutMs: Long = 5_000L) {
    waitUntil(timeoutMs) { onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }
}

fun ComposeTestRule.waitUntilTagExists(tag: String, timeoutMs: Long = 5_000L) {
    waitUntilExists(hasTestTag(tag), timeoutMs)
}

fun ComposeTestRule.waitUntilTagGone(tag: String, timeoutMs: Long = 5_000L) {
    waitUntilGone(hasTestTag(tag), timeoutMs)
}
