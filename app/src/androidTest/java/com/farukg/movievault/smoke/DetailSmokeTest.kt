package com.farukg.movievault.smoke

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.ui.testing.TestTags
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.di.DatabaseModule
import com.farukg.movievault.data.di.NetworkModule
import com.farukg.movievault.di.AppModule
import com.farukg.movievault.testing.TestMovies
import com.farukg.movievault.testing.cacheUpdated
import com.farukg.movievault.testing.movieDetail
import com.farukg.movievault.testing.seedCatalogEntities
import com.farukg.movievault.testing.waitUntilTagExists
import com.farukg.movievault.testing.waitUntilTagGone
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(NetworkModule::class, DatabaseModule::class, AppModule::class)
@RunWith(AndroidJUnit4::class)
class DetailSmokeTest : SmokeTestBase() {

    @Test
    fun partialCachedDetail_refreshFails_showsRetryBanner_thenRetryLoadsFullDetail() {
        runBlocking {
            movieDao.upsertAll(seedCatalogEntities(1L..1L))
            cacheDao.upsert(cacheUpdated(CacheKeys.CATALOG_LAST_UPDATED, clock.now()))
        }
        fakeRemote.enqueueDetail(movieId = 1L, result = AppResult.Error(AppError.Offline()))
        fakeRemote.enqueueDetail(movieId = 1L, result = AppResult.Success(movieDetail(1L)))

        launchApp()

        val item1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(item1Tag)
        composeRule.onNodeWithTag(item1Tag).performClick()

        val f1 = TestMovies.fixture(1L)
        composeRule.waitUntilTagExists(TestTags.DETAIL_SCREEN)
        composeRule.onNodeWithTag(TestTags.DETAIL_TITLE).assertTextContains(f1.title)

        composeRule.waitUntilTagExists(TestTags.DETAIL_ERROR_BANNER)
        composeRule.onNodeWithTag(TestTags.DETAIL_ERROR_RETRY).assertIsEnabled()
        composeRule.onNodeWithTag(TestTags.DETAIL_ERROR_RETRY).performClick()

        composeRule.waitUntilTagGone(TestTags.DETAIL_ERROR_BANNER)
        composeRule.onNodeWithTag(TestTags.DETAIL_ERROR_BANNER).assertDoesNotExist()

        composeRule.onNodeWithTag(TestTags.DETAIL_OVERVIEW).assertTextContains(f1.overview)
        composeRule.onNodeWithTag(TestTags.DETAIL_RUNTIME).assertTextContains(f1.runtimeText)
        composeRule
            .onNodeWithTag(TestTags.DETAIL_GENRES)
            .assert(hasAnyDescendant(hasText(f1.genres.first())))
    }
}
