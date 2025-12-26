package com.farukg.movievault.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.test.movieEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MovieDaoTest {

    private lateinit var db: MovieVaultDatabase
    private lateinit var movieDao: MovieDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        movieDao = db.movieDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeCatalog_returns_movies_ordered_by_popularRank() = runTest {
        movieDao.upsertAll(
            listOf(
                movieEntity(id = 1, title = "A", popularRank = 2),
                movieEntity(id = 2, title = "B", popularRank = 0),
                movieEntity(id = 3, title = "C", popularRank = 1),
            )
        )

        movieDao.observeCatalog().test {
            val first = awaitItem()
            assertEquals(listOf(2L, 3L, 1L), first.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeMovie_emits_null_then_entity_after_insert() = runTest {
        val targetId = 99L

        movieDao.observeMovie(targetId).test {
            val first = awaitItem()
            assertEquals(null, first)

            movieDao.upsert(movieEntity(id = targetId, title = "X", popularRank = 0))

            val second = awaitItem()
            requireNotNull(second)
            assertEquals(targetId, second.id)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
