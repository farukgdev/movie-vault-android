package com.farukg.movievault.smoke

import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class NavigationSmokeTest : SmokeTestBase() {

    @Test
    fun doubleTapCatalogItem_opensSingleDetail_backReturnsToCatalog() {
        fakeRemote.enqueuePopular(
            page = 1,
            result = AppResult.Success(moviesPage(page = 1, totalPages = 1, ids = 1L..20L)),
        )
        fakeRemote.enqueueDetail(movieId = 1L, result = AppResult.Success(movieDetail(1L)))

        launchApp()

        val item1Tag = TestTags.CATALOG_ITEM + 1L
        composeRule.waitUntilTagExists(item1Tag)

        val center = composeRule.onNodeWithTag(item1Tag).fetchSemanticsNode().boundsInRoot.center

        composeRule.onRoot().performTouchInput {
            click(center)
            advanceEventTime(30)
            click(center)
        }

        composeRule.waitUntilTagExists(TestTags.DETAIL_SCREEN)
        composeRule.onNodeWithTag(TestTags.DETAIL_BACK_BUTTON).performClick()

        // No double-stack
        composeRule.waitUntilTagExists(TestTags.CATALOG_SCREEN)
        composeRule.onNodeWithTag(TestTags.DETAIL_SCREEN).assertDoesNotExist()
        composeRule.onNodeWithTag(item1Tag).assertExists()
    }
}
