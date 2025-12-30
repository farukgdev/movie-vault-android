package com.farukg.movievault.data.repository.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.data.test.movieEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomCatalogRepositoryDetailRankTest {

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
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `movieDetail inserts NOT_IN_CATALOG row and catalog stream does not include it`() =
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

            repo.movieDetail(99L).test {
                var item = awaitItem()
                while (item !is AppResult.Success) item = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val stored = db.movieDao().getMovie(99L)!!
            assertEquals(-1, stored.popularRank)

            // catalog should only have the catalog movie
            assertEquals(1, db.movieDao().countCatalogMovies())
            val catalogMovie = db.movieDao().getMovie(1L)!!
            assertEquals(0, catalogMovie.popularRank)
        }

    private class FakeRemote : CatalogRemoteDataSource {
        var page: AppResult<MoviesPage> =
            AppResult.Success(MoviesPage(page = 1, totalPages = 1, results = emptyList()))
        var detail: AppResult<MovieDetail> = AppResult.Success(MovieDetail(id = 1, title = "x"))

        override suspend fun fetchPopularPage(page: Int): AppResult<MoviesPage> = this.page

        override suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail> = detail
    }
}
