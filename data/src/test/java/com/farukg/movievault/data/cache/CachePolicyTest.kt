package com.farukg.movievault.data.cache

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {

    private val staleAfterMs = CachePolicy.CATALOG_STALE_AFTER_MILLIS

    @Test
    fun `isCatalogStale returns true when lastUpdated is null`() {
        assertTrue(
            CachePolicy.isCatalogStale(lastUpdatedEpochMillis = null, nowEpochMillis = 1_000L)
        )
    }

    @Test
    fun `isCatalogStale returns false when age is below threshold`() {
        val now = 10_000_000L
        val lastUpdated = now - staleAfterMs + 1
        assertFalse(
            CachePolicy.isCatalogStale(lastUpdatedEpochMillis = lastUpdated, nowEpochMillis = now)
        )
    }

    @Test
    fun `isCatalogStale returns true when age equals threshold`() {
        val now = 10_000_000L
        val lastUpdated = now - staleAfterMs
        assertTrue(
            CachePolicy.isCatalogStale(lastUpdatedEpochMillis = lastUpdated, nowEpochMillis = now)
        )
    }

    @Test
    fun `shouldRefreshCatalog returns true when force is true even if not stale`() {
        val now = 10_000_000L
        val lastUpdated = now // fresh
        assertTrue(
            CachePolicy.shouldRefreshCatalog(
                force = true,
                lastUpdatedEpochMillis = lastUpdated,
                nowEpochMillis = now,
            )
        )
    }

    @Test
    fun `shouldRefreshCatalog returns false when not stale and not forced`() {
        val now = 10_000_000L
        val lastUpdated = now
        assertFalse(
            CachePolicy.shouldRefreshCatalog(
                force = false,
                lastUpdatedEpochMillis = lastUpdated,
                nowEpochMillis = now,
            )
        )
    }
}
