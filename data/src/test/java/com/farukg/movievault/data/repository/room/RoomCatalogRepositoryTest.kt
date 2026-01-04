package com.farukg.movievault.data.repository.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.data.repository.MovieDetailCacheState
import com.farukg.movievault.data.test.TestClock
import com.farukg.movievault.data.test.movieEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomCatalogRepositoryTest {

    private val clock = TestClock()
    private lateinit var db: MovieVaultDatabase
    private lateinit var repo: RoomCatalogRepository
    private lateinit var remote: FakeRemote

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        remote = FakeRemote()

        repo =
            RoomCatalogRepository(
                db = db,
                movieDao = db.movieDao(),
                favoriteDao = db.favoriteDao(),
                cacheMetadataDao = db.cacheMetadataDao(),
                remote = remote,
                remoteKeysDao = db.catalogRemoteKeysDao(),
                clock = clock,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `refreshMovieDetail inserts NOT_IN_CATALOG row and catalog stream does not include it`() =
        runTest {
            db.movieDao().upsert(movieEntity(id = 1L, title = "Catalog", popularRank = 0))

            remote.detail =
                AppResult.Success(
                    MovieDetail(
                        id = 99L,
                        title = "Detail Only",
                        releaseYear = 2020,
                        genres = emptyList(),
                        rating = 7.0,
                        overview = "x",
                        runtimeMinutes = 100,
                        posterUrl = null,
                        isFavorite = false,
                    )
                )

            val refreshed = repo.refreshMovieDetail(99L)
            assertTrue(refreshed is AppResult.Success)

            val stored = db.movieDao().getMovie(99L)!!
            assertEquals(-1, stored.popularRank)

            // catalog should only have the catalog movie
            assertEquals(1, db.movieDao().countCatalogMovies())
            val catalogMovie = db.movieDao().getMovie(1L)!!
            assertEquals(0, catalogMovie.popularRank)
        }

    @Test
    fun `refreshMovieDetail upserts detail fields and preserves popularRank`() = runTest {
        db.movieDao().upsert(movieEntity(id = 10L, title = "Catalog Movie", popularRank = 0))

        remote.detail =
            AppResult.Success(
                MovieDetail(
                    id = 10L,
                    title = "Catalog Movie",
                    releaseYear = 2021,
                    genres = listOf("Action"),
                    rating = 7.5,
                    overview = "Updated overview",
                    runtimeMinutes = 111,
                    posterUrl = null,
                    isFavorite = false,
                )
            )

        val result = repo.refreshMovieDetail(10L)
        assertTrue(result is AppResult.Success)

        val stored = db.movieDao().getMovie(10L)!!
        assertEquals(0, stored.popularRank) // preserved
        assertEquals("Updated overview", stored.overview)
        assertEquals(111, stored.runtimeMinutes)
        assertEquals(listOf("Action"), stored.genres)

        val state = repo.movieDetailCacheState(10L)
        assertEquals(MovieDetailCacheState.Fetched, state)
    }

    @Test
    fun `movieDetailCacheState returns Missing when no row exists`() = runTest {
        val state = repo.movieDetailCacheState(1234L)
        assertEquals(MovieDetailCacheState.Missing, state)
    }

    @Test
    fun `movieDetailCacheState transitions to Fetched after refreshMovieDetail succeeds`() =
        runTest {
            db.movieDao().upsert(movieEntity(id = 77L, title = "Partial", popularRank = -1))

            val before = repo.movieDetailCacheState(77L)
            assertTrue(before == MovieDetailCacheState.Partial)

            remote.detail =
                AppResult.Success(
                    MovieDetail(
                        id = 77L,
                        title = "Partial",
                        releaseYear = 2022,
                        genres = emptyList(),
                        rating = null,
                        overview = "",
                        runtimeMinutes = null,
                        posterUrl = null,
                        isFavorite = false,
                    )
                )

            val res = repo.refreshMovieDetail(77L)
            assertTrue(res is AppResult.Success)

            val after = repo.movieDetailCacheState(77L)
            assertEquals(MovieDetailCacheState.Fetched, after)
        }

    private class FakeRemote : CatalogRemoteDataSource {
        var page: AppResult<MoviesPage> =
            AppResult.Success(MoviesPage(page = 1, totalPages = 1, results = emptyList()))
        var detail: AppResult<MovieDetail> = AppResult.Success(MovieDetail(id = 1, title = "x"))

        override suspend fun fetchPopularPage(page: Int): AppResult<MoviesPage> = this.page

        override suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail> = detail
    }
}
