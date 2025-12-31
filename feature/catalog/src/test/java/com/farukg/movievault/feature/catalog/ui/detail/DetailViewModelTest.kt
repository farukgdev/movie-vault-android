package com.farukg.movievault.feature.catalog.ui.detail

import androidx.paging.PagingData
import app.cash.turbine.test
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.data.repository.FavoritesRepository
import com.farukg.movievault.feature.catalog.testing.MainDispatcherRule
import java.util.Locale
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
                TestCatalogRepository(initial = AppResult.Success(sampleDetail(isFavorite = false)))
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.uiState.test {
                assertEquals(DetailUiState.Loading, awaitItem())
                expectNoEvents() // no id -> no upstream -> no further emissions
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(0, catalogRepo.movieDetailCalls)
        }

    @Test
    fun `setMovieId triggers detail fetch and maps Success to Content`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val catalogRepo =
                TestCatalogRepository(
                    initial =
                        AppResult.Success(
                            sampleDetail(
                                title = "Movie A",
                                genres = listOf("Action", "Drama", "Comedy"),
                                releaseYear = 2024,
                                rating = 8.1,
                                runtimeMinutes = 120,
                                overview = "", // should fallback
                                isFavorite = true,
                            )
                        )
                )
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.uiState.test {
                // Initially loading (id not set)
                assertEquals(DetailUiState.Loading, awaitItem())

                vm.setMovieId(10)
                scheduler.runCurrent()

                val state = awaitItem()
                assertTrue(state is DetailUiState.Content)

                assertEquals(
                    DetailUiState.Content(
                        title = "Movie A",
                        metaPrimary = "Action, Drama • 2024", // first 2 genres + year
                        metaSecondary = "★ 8.1 • 120m",
                        overview = "No overview available.", // fallback
                        isFavorite = true,
                        posterUrl = null,
                        posterFallbackUrl = null,
                    ),
                    state,
                )

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, catalogRepo.movieDetailCalls)
            assertEquals(10L, catalogRepo.lastRequestedId)
        }

    @Test
    fun `maps Error to Error uiState`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val err = AppError.Network()
            val catalogRepo = TestCatalogRepository(initial = AppResult.Error(err))
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.uiState.test {
                assertEquals(DetailUiState.Loading, awaitItem())

                vm.setMovieId(1)
                scheduler.runCurrent()

                val state = awaitItem()
                assertEquals(DetailUiState.Error(err), state)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleFavorite does nothing before movieId is set`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val catalogRepo =
                TestCatalogRepository(initial = AppResult.Success(sampleDetail(isFavorite = false)))
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.toggleFavorite()
            scheduler.advanceUntilIdle()

            assertEquals(emptyList<Long>(), favoritesRepo.toggledIds)
        }

    @Test
    fun `toggleFavorite calls repository with current movieId`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val catalogRepo =
                TestCatalogRepository(initial = AppResult.Success(sampleDetail(isFavorite = false)))
            val favoritesRepo = TestFavoritesRepository()
            val vm = DetailViewModel(catalogRepo, favoritesRepo)

            vm.setMovieId(42)
            scheduler.runCurrent()

            vm.toggleFavorite()
            scheduler.advanceUntilIdle()

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
        isFavorite: Boolean = false,
    ): MovieDetail =
        MovieDetail(
            id = 1L,
            title = title,
            overview = overview,
            genres = genres,
            releaseYear = releaseYear,
            rating = rating,
            runtimeMinutes = runtimeMinutes,
            isFavorite = isFavorite,
        )

    private class TestCatalogRepository(initial: AppResult<MovieDetail>) : CatalogRepository {
        private val detailFlow = MutableStateFlow(initial)

        var movieDetailCalls: Int = 0
            private set

        var lastRequestedId: Long? = null
            private set

        override fun catalogPaging(): Flow<PagingData<Movie>> = flowOf(PagingData.empty())

        override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> {
            movieDetailCalls++
            lastRequestedId = movieId
            return detailFlow
        }

        override fun catalogLastUpdatedEpochMillis(): Flow<Long?> = flowOf(null)

        override suspend fun isCatalogStale(nowEpochMillis: Long): Boolean = false
    }

    private class TestFavoritesRepository : FavoritesRepository {
        val toggledIds = mutableListOf<Long>()
        val toggleResults = mutableListOf<Boolean>()
        private val favoriteState = mutableMapOf<Long, Boolean>()

        override fun favorites() = flowOf(AppResult.Success(emptyList<Movie>()))

        override suspend fun toggleFavorite(movieId: Long): AppResult<Boolean> {
            toggledIds += movieId
            val newValue = !(favoriteState[movieId] ?: false)
            favoriteState[movieId] = newValue
            toggleResults += newValue
            return AppResult.Success(newValue)
        }
    }
}
