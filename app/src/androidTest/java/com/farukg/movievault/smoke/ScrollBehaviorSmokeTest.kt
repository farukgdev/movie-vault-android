package com.farukg.movievault.smoke

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.ui.testing.TestTags
import com.farukg.movievault.data.di.DatabaseModule
import com.farukg.movievault.data.di.NetworkModule
import com.farukg.movievault.di.AppModule
import com.farukg.movievault.testing.movieDetail
import com.farukg.movievault.testing.moviesPage
import com.farukg.movievault.testing.waitUntilTagExists
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(NetworkModule::class, DatabaseModule::class, AppModule::class)
@RunWith(AndroidJUnit4::class)
class ScrollBehaviorSmokeTest : SmokeTestBase() {

    @Test
    fun scroll_restoreAfterBack_refreshScrollsToTop_onlyAfterSuccessfulRefresh() {
        fakeRemote.enqueuePopular(
            page = 1,
            result = AppResult.Success(moviesPage(page = 1, totalPages = 2, ids = 1L..20L)),
        )
        fakeRemote.enqueuePopular(
            page = 2,
            result = AppResult.Success(moviesPage(page = 2, totalPages = 2, ids = 21L..40L)),
        )
        fakeRemote.enqueueDetail(movieId = 25L, result = AppResult.Success(movieDetail(25L)))

        fakeRemote.enqueuePopular(page = 1, result = AppResult.Error(AppError.Offline()))
        fakeRemote.enqueuePopular(
            page = 1,
            result = AppResult.Success(moviesPage(page = 1, totalPages = 2, ids = 1L..20L)),
        )
        fakeRemote.enqueuePopular(
            page = 2,
            result = AppResult.Success(moviesPage(page = 2, totalPages = 2, ids = 21L..40L)),
        )

        launchApp()

        val item1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(item1Tag)
        composeRule.onNodeWithTag(item1Tag).assertExists()

        val item25Tag = TestTags.CATALOG_ITEM + 25L
        composeRule.onNodeWithTag(TestTags.CATALOG_GRID).performScrollToIndex(24)
        composeRule.waitUntilTagExists(item25Tag)
        composeRule.onNodeWithTag(item25Tag).assertIsDisplayed()

        composeRule.onNodeWithTag(item25Tag).performClick()
        composeRule.waitUntilTagExists(TestTags.DETAIL_SCREEN)
        pressBack()
        composeRule.waitUntilTagExists(TestTags.CATALOG_SCREEN)

        // Still visible
        composeRule.onNodeWithTag(item25Tag).assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.STATUS_BUTTON).performClick()
        composeRule.waitUntilTagExists(TestTags.CATALOG_STATUS_SHEET_CONTENT)
        composeRule.onNodeWithTag(TestTags.STATUS_SHEET_REFRESH).performClick()

        // No jump to top on failure
        composeRule.waitUntilTagExists(item25Tag)
        composeRule.onNodeWithTag(item25Tag).assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.CATALOG_SNACKBAR_ACTION).performClick()

        // After success: should be at top
        composeRule.waitUntilTagExists(item1Tag)
        composeRule.onNodeWithTag(item1Tag).assertIsDisplayed()
    }
}
