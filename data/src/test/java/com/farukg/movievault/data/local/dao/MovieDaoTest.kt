package com.farukg.movievault.data.local.dao

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.FavoriteEntity
import com.farukg.movievault.data.local.model.CatalogMovieRow
import com.farukg.movievault.data.test.movieEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MovieDaoTest {

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
    fun catalogPagingSource_filters_rank_orders_by_popularRank_and_is_not_affected_by_favorites_changes() =
        runTest {
            movieDao.upsertAll(
                listOf(
                    movieEntity(id = 1, title = "A", popularRank = 2),
                    movieEntity(id = 2, title = "B", popularRank = 0),
                    movieEntity(id = 3, title = "C", popularRank = 1),
                    movieEntity(id = 99, title = "DetailOnly", popularRank = -1),
                )
            )

            suspend fun loadIds(): List<Long> {
                val pagingSource = movieDao.catalogPagingSource()
                val loadResult =
                    pagingSource.load(
                        PagingSource.LoadParams.Refresh(
                            key = null,
                            loadSize = 50,
                            placeholdersEnabled = false,
                        )
                    )
                assertTrue(loadResult is PagingSource.LoadResult.Page)
                val page = loadResult as PagingSource.LoadResult.Page<Int, CatalogMovieRow>
                return page.data.map { it.id }
            }

            val before = loadIds()
            assertEquals(listOf(2L, 3L, 1L), before)

            // changing favorites should not affect catalog paging results
            favoriteDao.insert(FavoriteEntity(movieId = 3L, createdAtEpochMillis = 100L))
            val afterInsert = loadIds()
            assertEquals(before, afterInsert)

            favoriteDao.delete(3L)
            val afterDelete = loadIds()
            assertEquals(before, afterDelete)
        }

    @Test
    fun countCatalogMovies_counts_only_catalog_rows() = runTest {
        movieDao.upsertAll(
            listOf(
                movieEntity(id = 1, title = "A", popularRank = 0),
                movieEntity(id = 2, title = "B", popularRank = 5),
                movieEntity(id = 99, title = "DetailOnly", popularRank = -1),
            )
        )

        val count = movieDao.countCatalogMovies()
        assertEquals(2, count)
    }

    @Test
    fun clearCatalogRanks_sets_catalog_ranks_to_minus_one_without_deleting_rows() = runTest {
        movieDao.upsertAll(
            listOf(
                movieEntity(id = 1, title = "A", popularRank = 0),
                movieEntity(id = 2, title = "B", popularRank = 1),
                movieEntity(id = 99, title = "DetailOnly", popularRank = -1),
            )
        )

        assertEquals(2, movieDao.countCatalogMovies())
        assertEquals(3, movieDao.countMovies())

        movieDao.clearCatalogRanks()
        assertEquals(0, movieDao.countCatalogMovies())
        // rows still exist
        assertEquals(3, movieDao.countMovies())
        assertEquals(-1, movieDao.getMovie(1L)!!.popularRank)
        assertEquals(-1, movieDao.getMovie(2L)!!.popularRank)
        assertEquals(-1, movieDao.getMovie(99L)!!.popularRank)
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
