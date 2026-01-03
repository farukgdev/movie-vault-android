package com.farukg.movievault.feature.catalog.ui.detail

import androidx.paging.PagingData
import app.cash.turbine.test
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.data.repository.FavoritesRepository
import com.farukg.movievault.data.repository.MovieDetailCacheState
import com.farukg.movievault.feature.catalog.testing.MainDispatcherRule
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DetailViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `uiState stays Loading until movieId is set`() =
        runTest(mainDispatcherRule.dispatcher) {
            val catalogRepo =
                TestCatalogRepository(
                    initialDetail = AppResult.Success(sampleDetail(isFavorite = false)),
                    cacheState = MovieDetailCacheState.Fetched,
                )
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.uiState.test {
                assertEquals(DetailUiState.Loading(isRefreshing = false), awaitItem())
                expectNoEvents() // no id -> no upstream -> no further emissions
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(0, catalogRepo.movieDetailCalls)
            assertEquals(0, catalogRepo.cacheStateCalls)
            assertEquals(0, catalogRepo.refreshCalls)
        }

    @Test
    fun `setMovieId triggers detail observe and maps Success to Content`() =
        runTest(mainDispatcherRule.dispatcher) {
            val catalogRepo =
                TestCatalogRepository(
                    initialDetail =
                        AppResult.Success(
                            sampleDetail(
                                title = "Movie A",
                                genres = listOf("Action", "Drama", "Comedy"),
                                releaseYear = 2024,
                                rating = 8.1,
                                runtimeMinutes = 120,
                                overview = "",
                                isFavorite = true,
                                detailFetchedAtEpochMillis = 123L,
                            )
                        ),
                    cacheState = MovieDetailCacheState.Fetched,
                    refreshResult = AppResult.Success(Unit),
                )
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.uiState.test {
                assertEquals(DetailUiState.Loading(isRefreshing = false), awaitItem())

                vm.setMovieId(10)
                testScheduler.advanceUntilIdle()

                val state = awaitItem()
                assertTrue(state is DetailUiState.Content)

                assertEquals(
                    DetailUiState.Content(
                        title = "Movie A",
                        genres = listOf("Action", "Drama", "Comedy"),
                        releaseYear = 2024,
                        rating = 8.1,
                        runtimeMinutes = 120,
                        overview = "",
                        posterUrl = null,
                        posterFallbackUrl = null,
                        isFavorite = true,
                        hasFetchedDetail = true,
                        isRefreshing = false,
                        bannerError = null,
                        hasAttemptedRefresh = false,
                    ),
                    state,
                )

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, catalogRepo.movieDetailCalls)
            assertEquals(10L, catalogRepo.lastRequestedId)
            assertEquals(1, catalogRepo.cacheStateCalls)
            assertEquals(0, catalogRepo.refreshCalls) // no auto-refresh when cache is Fetched
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `maps Error to NoCacheError when cache state is Missing and refresh fails`() =
        runTest(mainDispatcherRule.dispatcher) {
            val flowErr = AppError.Network()
            val refreshErr = AppError.Offline()

            val catalogRepo =
                TestCatalogRepository(
                    initialDetail = AppResult.Error(flowErr),
                    cacheState = MovieDetailCacheState.Missing,
                    refreshResult = AppResult.Error(refreshErr),
                )
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.uiState.test {
                assertEquals(DetailUiState.Loading(isRefreshing = false), awaitItem())

                vm.setMovieId(1)
                testScheduler.runCurrent()

                assertEquals(DetailUiState.Loading(isRefreshing = true), awaitItem())

                testScheduler.advanceTimeBy(MIN_REFRESH_VISIBLE_MS)
                testScheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(DetailUiState.NoCacheError(error = refreshErr), state)

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, catalogRepo.movieDetailCalls)
            assertEquals(1, catalogRepo.cacheStateCalls)
            assertEquals(1, catalogRepo.refreshCalls)
        }

    @Test
    fun `toggleFavorite does nothing before movieId is set`() =
        runTest(mainDispatcherRule.dispatcher) {
            val catalogRepo =
                TestCatalogRepository(
                    initialDetail = AppResult.Success(sampleDetail(isFavorite = false)),
                    cacheState = MovieDetailCacheState.Fetched,
                )
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.toggleFavorite()
            testScheduler.advanceUntilIdle()

            assertEquals(emptyList<Long>(), favoritesRepo.toggledIds)
        }

    @Test
    fun `toggleFavorite calls repository with current movieId`() =
        runTest(mainDispatcherRule.dispatcher) {
            val catalogRepo =
                TestCatalogRepository(
                    initialDetail = AppResult.Success(sampleDetail(isFavorite = false)),
                    cacheState = MovieDetailCacheState.Fetched,
                )
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.setMovieId(42)
            testScheduler.advanceUntilIdle()

            vm.toggleFavorite()
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(42L), favoritesRepo.toggledIds)
        }

    // ---- Helpers

    private fun sampleDetail(
        title: String = "T",
        genres: List<String> = listOf("Genre1"),
        releaseYear: Int? = 2024,
        rating: Double? = null,
        runtimeMinutes: Int? = null,
        overview: String = "Overview",
        posterUrl: String? = null,
        isFavorite: Boolean = false,
        detailFetchedAtEpochMillis: Long? = null,
    ): MovieDetail =
        MovieDetail(
            id = 1L,
            title = title,
            overview = overview,
            genres = genres,
            releaseYear = releaseYear,
            rating = rating,
            runtimeMinutes = runtimeMinutes,
            posterUrl = posterUrl,
            isFavorite = isFavorite,
            detailFetchedAtEpochMillis = detailFetchedAtEpochMillis,
        )

    private class TestCatalogRepository(
        initialDetail: AppResult<MovieDetail>,
        private val cacheState: MovieDetailCacheState,
        private val refreshResult: AppResult<Unit> = AppResult.Success(Unit),
    ) : CatalogRepository {

        private val detailFlow = MutableStateFlow(initialDetail)

        var movieDetailCalls: Int = 0
            private set

        var cacheStateCalls: Int = 0
            private set

        var refreshCalls: Int = 0
            private set

        var lastRequestedId: Long? = null
            private set

        override fun catalogPaging(): Flow<PagingData<Movie>> = flowOf(PagingData.empty())

        override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> {
            movieDetailCalls++
            lastRequestedId = movieId
            return detailFlow
        }

        override suspend fun refreshMovieDetail(movieId: Long): AppResult<Unit> {
            refreshCalls++
            return refreshResult
        }

        override suspend fun movieDetailCacheState(movieId: Long): MovieDetailCacheState {
            cacheStateCalls++
            return cacheState
        }

        override fun catalogLastUpdatedEpochMillis(): Flow<Long?> = flowOf(null)

        override suspend fun isCatalogStale(nowEpochMillis: Long): Boolean = false

        override suspend fun hasCatalogCache(): Boolean = false
    }

    private class TestFavoritesRepository : FavoritesRepository {
        val toggledIds = mutableListOf<Long>()
        private val favoriteState = mutableMapOf<Long, Boolean>()

        override fun favorites() = flowOf(AppResult.Success(emptyList<Movie>()))

        override suspend fun toggleFavorite(movieId: Long): AppResult<Boolean> {
            toggledIds += movieId
            val newValue = !(favoriteState[movieId] ?: false)
            favoriteState[movieId] = newValue
            return AppResult.Success(newValue)
        }
    }
}
