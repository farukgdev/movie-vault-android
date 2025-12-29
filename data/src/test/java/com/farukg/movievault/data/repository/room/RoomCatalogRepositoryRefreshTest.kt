package com.farukg.movievault.data.repository.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.CatalogRemoteKeysDao
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import com.farukg.movievault.data.local.entity.MovieEntity
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomCatalogRepositoryRefreshTest {

    private lateinit var db: MovieVaultDatabase
    private lateinit var movieDao: MovieDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var cacheDao: CacheMetadataDao
    private lateinit var remoteKeysDao: CatalogRemoteKeysDao
    private lateinit var remote: FakeRemote

    private lateinit var repo: RoomCatalogRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        movieDao = db.movieDao()
        favoriteDao = db.favoriteDao()
        cacheDao = db.cacheMetadataDao()
        remoteKeysDao = db.catalogRemoteKeysDao()
        remote = FakeRemote()

        repo =
            RoomCatalogRepository(
                db = db,
                movieDao = movieDao,
                favoriteDao = favoriteDao,
                cacheMetadataDao = cacheDao,
                remote = remote,
                remoteKeysDao = remoteKeysDao,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `refreshCatalog skips remote when not stale and not forced`() = runTest {
        val now = System.currentTimeMillis()
        cacheDao.upsert(
            CacheMetadataEntity(CacheKeys.CATALOG_LAST_UPDATED, lastUpdatedEpochMillis = now)
        )

        remote.page1Result =
            AppResult.Success(
                MoviesPage(
                    page = 1,
                    totalPages = 1,
                    results = listOf(Movie(id = 1L, title = "ShouldNotBeUsed", releaseYear = 2024)),
                )
            )

        val result = repo.refreshCatalog(force = false)

        assertTrue(result is AppResult.Success)
        assertEquals(0, remote.fetchPopularPageCalls)
    }

    @Test
    fun `refreshCatalog forces remote when force true even if not stale`() = runTest {
        val now = System.currentTimeMillis()
        cacheDao.upsert(
            CacheMetadataEntity(CacheKeys.CATALOG_LAST_UPDATED, lastUpdatedEpochMillis = now)
        )

        remote.page1Result =
            AppResult.Success(
                MoviesPage(
                    page = 1,
                    totalPages = 1,
                    results = listOf(Movie(id = 1L, title = "FromRemote", releaseYear = 2024)),
                )
            )

        val result = repo.refreshCatalog(force = true)

        assertTrue(result is AppResult.Success)
        assertEquals(1, remote.fetchPopularPageCalls)

        val stored = movieDao.getMovie(1L)
        assertNotNull(stored)
        assertEquals("FromRemote", stored!!.title)
    }

    @Test
    fun `stale refresh success upserts catalog and preserves existing detail fields`() = runTest {
        movieDao.upsert(
            MovieEntity(
                id = 7L,
                title = "OldTitle",
                releaseYear = 2020,
                posterUrl = null,
                rating = 6.0,
                overview = "Existing overview",
                runtimeMinutes = 99,
                genres = listOf("Drama"),
                popularRank = 123,
            )
        )

        cacheDao.upsert(
            CacheMetadataEntity(CacheKeys.CATALOG_LAST_UPDATED, lastUpdatedEpochMillis = 0L)
        )

        remote.page1Result =
            AppResult.Success(
                MoviesPage(
                    page = 1,
                    totalPages = 1,
                    results =
                        listOf(
                            Movie(
                                id = 7L,
                                title = "NewTitle",
                                releaseYear = 2021,
                                posterUrl = "p",
                                rating = 8.5,
                                isFavorite = false,
                            )
                        ),
                )
            )

        val start = System.currentTimeMillis()
        val result = repo.refreshCatalog(force = false)
        val end = System.currentTimeMillis()

        assertTrue(result is AppResult.Success)
        assertEquals(1, remote.fetchPopularPageCalls)

        val stored = movieDao.getMovie(7L)
        requireNotNull(stored)

        assertEquals("NewTitle", stored.title)
        assertEquals(2021, stored.releaseYear)
        assertEquals("p", stored.posterUrl)
        assertEquals(8.5, stored.rating)

        // preserved detail fields
        assertEquals("Existing overview", stored.overview)
        assertEquals(99, stored.runtimeMinutes)
        assertEquals(listOf("Drama"), stored.genres)

        // refreshed rank
        assertEquals(0, stored.popularRank)

        val lastUpdated = cacheDao.get(CacheKeys.CATALOG_LAST_UPDATED)?.lastUpdatedEpochMillis
        assertNotNull(lastUpdated)
        assertTrue(
            "lastUpdated must be within the refresh call window",
            lastUpdated!! in start..end,
        )
    }

    @Test
    fun `refresh failure does not wipe cached rows and does not advance lastUpdated but exposes refresh error`() =
        runTest {
            movieDao.upsert(
                MovieEntity(
                    id = 10L,
                    title = "Cached",
                    releaseYear = 2010,
                    posterUrl = null,
                    rating = 7.0,
                    overview = null,
                    runtimeMinutes = null,
                    genres = emptyList(),
                    popularRank = 0,
                )
            )

            val oldUpdated = 123L
            cacheDao.upsert(
                CacheMetadataEntity(
                    CacheKeys.CATALOG_LAST_UPDATED,
                    lastUpdatedEpochMillis = oldUpdated,
                )
            )

            val err = AppError.Network()
            remote.page1Result = AppResult.Error(err)

            repo.catalogRefreshState().test {
                val initial = awaitItem()
                assertEquals(oldUpdated, initial.lastUpdatedEpochMillis)
                assertNull(initial.lastRefreshError)
                assertEquals(false, initial.isRefreshing)

                val result = repo.refreshCatalog(force = false)
                assertTrue(result is AppResult.Error)

                // we should eventually see an error state
                var last = awaitItem()
                while (last.isRefreshing) {
                    last = awaitItem()
                }

                assertEquals(oldUpdated, last.lastUpdatedEpochMillis)
                assertTrue(last.lastRefreshError is AppError.Network)

                val stored = movieDao.getMovie(10L)
                requireNotNull(stored)
                assertEquals("Cached", stored.title)

                val still = cacheDao.get(CacheKeys.CATALOG_LAST_UPDATED)?.lastUpdatedEpochMillis
                assertEquals(oldUpdated, still)

                cancelAndIgnoreRemainingEvents()
            }
        }

    private class FakeRemote : CatalogRemoteDataSource {
        var fetchPopularPageCalls: Int = 0
            private set

        var page1Result: AppResult<MoviesPage> =
            AppResult.Success(MoviesPage(page = 1, totalPages = 1, results = emptyList()))

        override suspend fun fetchPopularPage(page: Int): AppResult<MoviesPage> {
            fetchPopularPageCalls++
            return page1Result
        }

        override suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail> {
            return AppResult.Error(AppError.Http(404))
        }
    }
}
