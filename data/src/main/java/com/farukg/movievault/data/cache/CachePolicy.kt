package com.farukg.movievault.data.cache

object CachePolicy {
    const val CATALOG_STALE_AFTER_MILLIS: Long = 60 * 60 * 1000L

    fun isCatalogStale(
        lastUpdatedEpochMillis: Long?,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (lastUpdatedEpochMillis == null) return true
        return (nowEpochMillis - lastUpdatedEpochMillis) >= CATALOG_STALE_AFTER_MILLIS
    }

    fun shouldRefreshCatalog(
        force: Boolean,
        lastUpdatedEpochMillis: Long?,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Boolean = force || isCatalogStale(lastUpdatedEpochMillis, nowEpochMillis)
}
