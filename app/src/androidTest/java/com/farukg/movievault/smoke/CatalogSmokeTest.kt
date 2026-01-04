package com.farukg.movievault.smoke

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.ui.testing.TestTags
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.cache.CachePolicy.CATALOG_STALE_AFTER_MILLIS
import com.farukg.movievault.data.di.DatabaseModule
import com.farukg.movievault.data.di.NetworkModule
import com.farukg.movievault.di.AppModule
import com.farukg.movievault.testing.cacheUpdated
import com.farukg.movievault.testing.moviesPage
import com.farukg.movievault.testing.seedCatalogEntities
import com.farukg.movievault.testing.waitUntilExists
import com.farukg.movievault.testing.waitUntilTagExists
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(NetworkModule::class, DatabaseModule::class, AppModule::class)
@RunWith(AndroidJUnit4::class)
class CatalogSmokeTest : SmokeTestBase() {

    @Test
    fun noCache_offline_showsFullscreenError_thenRetryLoadsCatalog() {
        fakeRemote.enqueuePopular(page = 1, result = AppResult.Error(AppError.Offline()))
        fakeRemote.enqueuePopular(
            page = 1,
            result = AppResult.Success(moviesPage(page = 1, totalPages = 1, ids = 1L..20L)),
        )

        launchApp()

        composeRule.waitUntilTagExists(TestTags.FULLSCREEN_ERROR)
        composeRule.onNodeWithTag(TestTags.CATALOG_FULLSCREEN_RETRY).performClick()

        val item1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(item1Tag)
        composeRule.onNodeWithTag(item1Tag).assertExists()
    }

    @Test
    fun freshCache_offline_showsCatalog_thenManualRefreshMarksOffline() {
        runBlocking {
            movieDao.upsertAll(seedCatalogEntities(1L..20L))
            val freshAt = clock.now() - 10_000L
            cacheDao.upsert(cacheUpdated(CacheKeys.CATALOG_LAST_UPDATED, freshAt))
        }

        fakeRemote.enqueuePopular(page = 1, result = AppResult.Error(AppError.Offline()))

        launchApp()

        val item1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(item1Tag)
        composeRule.onNodeWithTag(item1Tag).assertExists()

        // Status is not offline on launch
        composeRule.onNodeWithTag(TestTags.STATUS_BUTTON).assertExists()
        composeRule.onNodeWithContentDescription("Status: offline").assertDoesNotExist()

        composeRule.onNodeWithTag(TestTags.STATUS_BUTTON).performClick()
        composeRule.waitUntilTagExists(TestTags.STATUS_SHEET)
        composeRule.onNodeWithTag(TestTags.STATUS_SHEET_REFRESH).performClick()

        composeRule.waitUntilExists(hasContentDescription("Status: offline"))
        composeRule.onNodeWithTag(item1Tag).assertExists()
    }

    @Test
    fun staleCache_offline_autoRefreshFails_marksOffline_keepsCachedCatalog() {
        runBlocking {
            movieDao.upsertAll(seedCatalogEntities(1L..20L))
            val staleAt = clock.now() - (CATALOG_STALE_AFTER_MILLIS) - 10_000L
            cacheDao.upsert(cacheUpdated(CacheKeys.CATALOG_LAST_UPDATED, staleAt))
        }

        fakeRemote.enqueuePopular(page = 1, result = AppResult.Error(AppError.Offline()))

        launchApp()

        val item1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(item1Tag)
        composeRule.onNodeWithTag(item1Tag).assertExists()

        composeRule.waitUntilExists(hasContentDescription("Status: offline"))

        // Cache content must remain visible
        composeRule.onNodeWithTag(item1Tag).assertExists()
    }

    @Test
    fun manualRefresh_offline_showsSnackbar_thenRetryRecovers() {
        fakeRemote.enqueuePopular(
            page = 1,
            result = AppResult.Success(moviesPage(page = 1, totalPages = 1, ids = 1L..20L)),
        )
        fakeRemote.enqueuePopular(page = 1, result = AppResult.Error(AppError.Offline()))
        fakeRemote.enqueuePopular(
            page = 1,
            result = AppResult.Success(moviesPage(page = 1, totalPages = 1, ids = 1L..20L)),
        )

        launchApp()

        val item1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(item1Tag)

        composeRule.onNodeWithTag(TestTags.STATUS_BUTTON).performClick()
        composeRule.waitUntilTagExists(TestTags.STATUS_SHEET)
        composeRule.onNodeWithTag(TestTags.STATUS_SHEET_REFRESH).performClick()

        composeRule.waitUntilTagExists(TestTags.CATALOG_SNACKBAR_HOST)
        composeRule.onNodeWithTag(TestTags.CATALOG_SNACKBAR_MESSAGE).assertTextEquals("Offline")
        composeRule.onNodeWithTag(TestTags.CATALOG_SNACKBAR_ACTION).performClick()

        composeRule.waitUntilExists(hasContentDescription("Status: up to date"))
        composeRule.onNodeWithTag(item1Tag).assertExists()
    }
}
