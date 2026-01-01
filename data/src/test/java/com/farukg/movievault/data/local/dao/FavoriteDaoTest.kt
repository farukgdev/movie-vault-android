package com.farukg.movievault.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.FavoriteEntity
import com.farukg.movievault.data.test.movieEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoriteDaoTest {

    private lateinit var db: MovieVaultDatabase
    private lateinit var movieDao: MovieDao
    private lateinit var favoriteDao: FavoriteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        movieDao = db.movieDao()
        favoriteDao = db.favoriteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeFavoriteRows_returns_joined_movies_sorted_by_createdAt_desc() = runTest {
        movieDao.upsertAll(
            listOf(
                movieEntity(id = 1, title = "A", popularRank = 0),
                movieEntity(id = 2, title = "B", popularRank = 1),
            )
        )

        favoriteDao.insert(FavoriteEntity(movieId = 1, createdAtEpochMillis = 100))
        favoriteDao.insert(FavoriteEntity(movieId = 2, createdAtEpochMillis = 200))

        favoriteDao.observeFavoriteRows().test {
            val first = awaitItem()
            assertEquals(listOf(2L, 1L), first.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFavoriteIds_updates_when_favorite_deleted() = runTest {
        movieDao.upsert(movieEntity(id = 10, title = "X", popularRank = 0))

        favoriteDao.observeFavoriteIds().test {
            assertEquals(emptyList<Long>(), awaitItem())

            favoriteDao.insert(FavoriteEntity(movieId = 10, createdAtEpochMillis = 123))
            assertEquals(listOf(10L), awaitItem())

            favoriteDao.delete(10L)
            assertEquals(emptyList<Long>(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
