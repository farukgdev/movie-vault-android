package com.farukg.movievault.smoke

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.ui.testing.TestTags
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.di.DatabaseModule
import com.farukg.movievault.data.di.NetworkModule
import com.farukg.movievault.data.local.entity.FavoriteEntity
import com.farukg.movievault.di.AppModule
import com.farukg.movievault.testing.TestMovies
import com.farukg.movievault.testing.cacheUpdated
import com.farukg.movievault.testing.fullMovieEntity
import com.farukg.movievault.testing.movieDetail
import com.farukg.movievault.testing.moviesPage
import com.farukg.movievault.testing.waitUntilTagExists
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(NetworkModule::class, DatabaseModule::class, AppModule::class)
@RunWith(AndroidJUnit4::class)
class FavoritesSmokeTest : SmokeTestBase() {

    @Test
    fun favorite_roundTrip_addsToFavorites_thenRemoveShowsEmptyState() {
        fakeRemote.enqueuePopular(
            page = 1,
            result = AppResult.Success(moviesPage(page = 1, totalPages = 1, ids = 1L..20L)),
        )
        fakeRemote.enqueueDetail(movieId = 1L, result = AppResult.Success(movieDetail(1L)))

        launchApp()

        val catalogItem1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(catalogItem1Tag)
        composeRule.onNodeWithTag(catalogItem1Tag).assertExists()

        composeRule.onNodeWithTag(catalogItem1Tag).performClick()
        composeRule.waitUntilTagExists(TestTags.DETAIL_SCREEN)
        composeRule
            .onNodeWithTag(TestTags.DETAIL_OVERVIEW)
            .assertExists()
            .assertTextContains(TestMovies.overview(1L))

        composeRule.onNodeWithTag(TestTags.DETAIL_FAVORITE_BUTTON).performClick()
        composeRule.onNodeWithContentDescription("Unfavorite").assertExists()

        pressBack()
        composeRule.waitUntilTagExists(TestTags.CATALOG_SCREEN)

        composeRule.onNodeWithTag(TestTags.CATALOG_TOPBAR_FAVORITES_BUTTON).performClick()
        composeRule.waitUntilTagExists(TestTags.FAVORITES_SCREEN)

        val favItem1Tag = TestTags.FAVORITE_ITEM + 1L
        val favRemove1Tag = TestTags.FAVORITE_REMOVE + 1L
        composeRule.waitUntilTagExists(favItem1Tag)
        composeRule.onNodeWithTag(favItem1Tag).assertExists()

        composeRule.onNodeWithTag(favRemove1Tag).performClick()

        composeRule.waitUntilTagExists(TestTags.EMPTY_STATE)
        composeRule.onNodeWithTag(favItem1Tag).assertDoesNotExist()
    }

    @Test
    fun offline_openFavoriteDetail_usesCachedFullDetail_noErrorBanner() {
        runBlocking {
            val now = clock.now()
            cacheDao.upsert(cacheUpdated(CacheKeys.CATALOG_LAST_UPDATED, now))
            movieDao.upsert(fullMovieEntity(id = 7L, rank = 0, nowMs = now))
            favoriteDao.insert(FavoriteEntity(movieId = 7L, createdAtEpochMillis = now))
        }
        launchApp()

        composeRule.waitUntilTagExists(TestTags.CATALOG_SCREEN)
        composeRule.onNodeWithTag(TestTags.CATALOG_TOPBAR_FAVORITES_BUTTON).performClick()
        composeRule.waitUntilTagExists(TestTags.FAVORITES_SCREEN)

        // Open detail from favorites
        val favItem7Tag = TestTags.FAVORITE_ITEM + 7L
        composeRule.waitUntilTagExists(favItem7Tag)
        composeRule.onNodeWithTag(favItem7Tag).performClick()

        composeRule.waitUntilTagExists(TestTags.DETAIL_SCREEN)

        val f7 = TestMovies.fixture(7L)
        composeRule.onNodeWithTag(TestTags.DETAIL_OVERVIEW).assertTextContains(f7.overview)
        composeRule.onNodeWithTag(TestTags.DETAIL_RUNTIME).assertTextContains(f7.runtimeText)
        composeRule
            .onNodeWithTag(TestTags.DETAIL_GENRES)
            .assert(hasAnyDescendant(hasText(f7.genres.first())))

        composeRule.onNodeWithTag(TestTags.DETAIL_ERROR_BANNER).assertDoesNotExist()
    }
}
