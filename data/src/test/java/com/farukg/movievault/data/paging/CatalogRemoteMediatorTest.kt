package com.farukg.movievault.data.paging

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.error.AppErrorException
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.cache.CacheKeys
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import com.farukg.movievault.data.local.model.MovieWithFavorite
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.data.test.MainDispatcherRule
import com.farukg.movievault.data.test.movieEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class CatalogRemoteMediatorTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: MovieVaultDatabase
    private lateinit var remote: FakeRemote

    private lateinit var mediator: CatalogRemoteMediator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        remote = FakeRemote()

        mediator =
            CatalogRemoteMediator(
                db = db,
                movieDao = db.movieDao(),
                remoteKeysDao = db.catalogRemoteKeysDao(),
                cacheMetadataDao = db.cacheMetadataDao(),
                remote = remote,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun refresh_writes_page1_ranks_keys_and_lastUpdated() = runTest {
        remote.pages[1] = MoviesPage(page = 1, totalPages = 2, results = movies(1, 20))

        val state = emptyPagingState()

        val result = mediator.load(LoadType.REFRESH, state)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue(!(result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)

        // DB contains catalog rows and ranks are stable
        assertEquals(0, db.movieDao().getMovie(1L)!!.popularRank)
        assertEquals(19, db.movieDao().getMovie(20L)!!.popularRank)

        val key = db.catalogRemoteKeysDao().remoteKeyByMovieId(1L)!!
        assertEquals(null, key.prevKey)
        assertEquals(2, key.nextKey)

        val lastUpdated =
            db.cacheMetadataDao().get(CacheKeys.CATALOG_LAST_UPDATED)?.lastUpdatedEpochMillis
        assertTrue(lastUpdated != null && lastUpdated > 0L)

        // pagingSource can read the newly cached page
        val page = loadCatalogPagingSource(loadSize = 50)
        assertEquals((1L..20L).toList(), page.map { it.movie.id })
    }

    @Test
    fun append_writes_page2_and_continues_ranks() = runTest {
        remote.pages[1] = MoviesPage(page = 1, totalPages = 2, results = movies(1, 20))
        mediator.load(LoadType.REFRESH, emptyPagingState())

        remote.pages[2] = MoviesPage(page = 2, totalPages = 2, results = movies(21, 40))

        val firstPage = loadCatalogPagingSource(loadSize = 20)
        val state =
            PagingState(
                pages =
                    listOf(
                        PagingSource.LoadResult.Page(data = firstPage, prevKey = null, nextKey = 2)
                    ),
                anchorPosition = firstPage.lastIndex,
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                leadingPlaceholderCount = 0,
            )

        val result = mediator.load(LoadType.APPEND, state)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)

        assertEquals(0, db.movieDao().getMovie(1L)!!.popularRank)
        assertEquals(19, db.movieDao().getMovie(20L)!!.popularRank)
        assertEquals(20, db.movieDao().getMovie(21L)!!.popularRank)
        assertEquals(39, db.movieDao().getMovie(40L)!!.popularRank)

        val all = loadCatalogPagingSource(loadSize = 100)
        assertEquals((1L..40L).toList(), all.map { it.movie.id })

        assertEquals(listOf(1, 2), remote.fetchPopularPageCalls)
    }

    @Test
    fun cached_and_fresh_catalog_skips_initial_refresh() = runTest {
        db.movieDao()
            .upsertAll(
                listOf(
                    movieEntity(id = 1, title = "Cached 1", popularRank = 0),
                    movieEntity(id = 2, title = "Cached 2", popularRank = 1),
                    movieEntity(id = 99, title = "DetailOnly", popularRank = -1),
                )
            )
        db.cacheMetadataDao()
            .upsert(
                CacheMetadataEntity(
                    key = CacheKeys.CATALOG_LAST_UPDATED,
                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                )
            )

        val action = mediator.initialize()
        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, action)
        assertEquals(emptyList<Int>(), remote.fetchPopularPageCalls)

        val page = loadCatalogPagingSource(loadSize = 50)
        assertEquals(listOf(1L, 2L), page.map { it.movie.id })
    }

    @Test
    fun remote_error_is_wrapped_as_AppErrorException() = runTest {
        remote.error = AppError.Network()

        val result = mediator.load(LoadType.REFRESH, emptyPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        val throwable = (result as RemoteMediator.MediatorResult.Error).throwable
        assertTrue(throwable is AppErrorException)
        assertTrue((throwable as AppErrorException).error is AppError.Network)
    }

    private suspend fun loadCatalogPagingSource(loadSize: Int): List<MovieWithFavorite> {
        val pagingSource = db.movieDao().catalogPagingSource()
        val result =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = loadSize,
                    placeholdersEnabled = false,
                )
            )
        require(result is PagingSource.LoadResult.Page)
        return result.data
    }

    private fun emptyPagingState(): PagingState<Int, MovieWithFavorite> =
        PagingState(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            leadingPlaceholderCount = 0,
        )

    private fun movies(startId: Int, endIdInclusive: Int): List<Movie> =
        (startId..endIdInclusive).map { id ->
            Movie(
                id = id.toLong(),
                title = "Movie $id",
                releaseYear = 2024,
                posterUrl = null,
                rating = 8.0,
                isFavorite = false,
            )
        }

    private class FakeRemote : CatalogRemoteDataSource {
        val pages: MutableMap<Int, MoviesPage> = mutableMapOf()
        var error: AppError? = null
        val fetchPopularPageCalls = mutableListOf<Int>()

        override suspend fun fetchPopularPage(page: Int): AppResult<MoviesPage> {
            fetchPopularPageCalls += page
            error?.let {
                return AppResult.Error(it)
            }
            return AppResult.Success(pages.getValue(page))
        }

        override suspend fun fetchMovieDetail(movieId: Long): AppResult<MovieDetail> {
            return AppResult.Error(AppError.Http(404))
        }
    }
}
