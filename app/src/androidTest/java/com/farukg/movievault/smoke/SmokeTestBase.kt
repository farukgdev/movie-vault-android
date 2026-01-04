package com.farukg.movievault.smoke

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.testing.FakeCatalogRemoteDataSource
import com.farukg.movievault.testing.HiltTestActivity
import com.farukg.movievault.testing.TestClock
import com.farukg.movievault.testing.TestMovieVaultApp
import dagger.hilt.android.testing.HiltAndroidRule
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain

open class SmokeTestBase {

    protected val hiltRule = HiltAndroidRule(this)
    protected val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @get:Rule val chain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject lateinit var fakeRemote: FakeCatalogRemoteDataSource
    @Inject lateinit var db: MovieVaultDatabase
    @Inject lateinit var movieDao: MovieDao
    @Inject lateinit var favoriteDao: FavoriteDao
    @Inject lateinit var cacheDao: CacheMetadataDao
    @Inject lateinit var clock: TestClock

    @Before
    fun baseSetUp() {
        hiltRule.inject()

        fakeRemote.reset()
        runBlocking { db.clearAllTables() }

        composeRule.mainClock.autoAdvance = true
    }

    protected fun launchApp() {
        composeRule.setContent { TestMovieVaultApp() }
    }
}
