package com.farukg.movievault.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CacheMetadataDaoTest {

    private lateinit var db: MovieVaultDatabase
    private lateinit var dao: CacheMetadataDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.cacheMetadataDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeLastUpdated_emits_null_then_value_then_updated_value() = runTest {
        dao.observeLastUpdated(CacheKeys.CATALOG_LAST_UPDATED).test {
            assertEquals(null, awaitItem())

            dao.upsert(
                CacheMetadataEntity(CacheKeys.CATALOG_LAST_UPDATED, lastUpdatedEpochMillis = 111L)
            )
            assertEquals(111L, awaitItem())

            dao.upsert(
                CacheMetadataEntity(CacheKeys.CATALOG_LAST_UPDATED, lastUpdatedEpochMillis = 222L)
            )
            assertEquals(222L, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
