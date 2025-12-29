package com.farukg.movievault.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.CatalogRemoteKeyEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogRemoteKeysDaoTest {

    private lateinit var db: MovieVaultDatabase
    private lateinit var dao: CatalogRemoteKeysDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.catalogRemoteKeysDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_read_remote_key() = runTest {
        dao.insertAll(listOf(CatalogRemoteKeyEntity(movieId = 10L, prevKey = null, nextKey = 2)))

        val key = dao.remoteKeyByMovieId(10L)
        requireNotNull(key)
        assertEquals(10L, key.movieId)
        assertEquals(null, key.prevKey)
        assertEquals(2, key.nextKey)
    }

    @Test
    fun clearRemoteKeys_removes_rows() = runTest {
        dao.insertAll(
            listOf(
                CatalogRemoteKeyEntity(movieId = 1L, prevKey = null, nextKey = 2),
                CatalogRemoteKeyEntity(movieId = 2L, prevKey = 1, nextKey = 3),
            )
        )

        dao.clearRemoteKeys()

        assertNull(dao.remoteKeyByMovieId(1L))
        assertNull(dao.remoteKeyByMovieId(2L))
    }
}
