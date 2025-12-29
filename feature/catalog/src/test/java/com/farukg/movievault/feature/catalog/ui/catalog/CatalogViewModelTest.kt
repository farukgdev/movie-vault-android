package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.paging.PagingData
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.repository.CatalogRefreshState
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.feature.catalog.testing.MainDispatcherRule
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CatalogViewModelTest {

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
    fun `maps success non-empty to Content with expected row mapping`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                TestCatalogRepository(
                    initial =
                        AppResult.Success(listOf(Movie(id = 1, title = "A", releaseYear = 2024)))
                )

            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.uiState.test {
                val state = awaitSettled(testScheduler)
                assertEquals(
                    CatalogUiState.Content(
                        movies = listOf(MovieRowUi(id = 1, title = "A", subtitle = "2024"))
                    ),
                    state,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `maps success empty to Empty`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = TestCatalogRepository(initial = AppResult.Success(emptyList()))
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.uiState.test {
                val state = awaitSettled(testScheduler)
                assertEquals(CatalogUiState.Empty, state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `maps error to Error with same AppError instance`() =
        runTest(mainDispatcherRule.dispatcher) {
            val err = AppError.Network()
            val repo = TestCatalogRepository(initial = AppResult.Error(err))
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.uiState.test {
                val state = awaitSettled(testScheduler)

                assertTrue(state is CatalogUiState.Error)
                assertEquals(err, (state as CatalogUiState.Error).error)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits new UiState when repository flow updates`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = TestCatalogRepository(initial = AppResult.Success(emptyList()))
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.uiState.test {
                assertEquals(CatalogUiState.Empty, awaitSettled(testScheduler))

                repo.emit(AppResult.Success(listOf(Movie(id = 2, title = "B", releaseYear = 2023))))
                testScheduler.runCurrent()

                assertEquals(
                    CatalogUiState.Content(
                        movies = listOf(MovieRowUi(id = 2, title = "B", subtitle = "2023"))
                    ),
                    awaitItem(),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    private suspend fun ReceiveTurbine<CatalogUiState>.awaitSettled(
        scheduler: TestCoroutineScheduler
    ): CatalogUiState {
        val first = awaitItem()
        if (first != CatalogUiState.Loading) return first

        scheduler.advanceUntilIdle()
        return awaitItem()
    }

    private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock {
        @OptIn(ExperimentalCoroutinesApi::class) override fun now(): Long = scheduler.currentTime
    }

    private class TestCatalogRepository(initial: AppResult<List<Movie>>) : CatalogRepository {

        private val catalogFlow = MutableStateFlow(initial)

        fun emit(value: AppResult<List<Movie>>) {
            catalogFlow.value = value
        }

        override fun catalog(): Flow<AppResult<List<Movie>>> = catalogFlow

        override fun catalogPaging(): Flow<PagingData<Movie>> = flowOf(PagingData.empty())

        override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> =
            flowOf(AppResult.Error(AppError.Http(404)))

        private val refreshStateFlow =
            MutableStateFlow(
                CatalogRefreshState(
                    lastUpdatedEpochMillis = null,
                    isRefreshing = false,
                    lastRefreshError = null,
                )
            )

        override fun catalogRefreshState(): Flow<CatalogRefreshState> = refreshStateFlow

        override suspend fun refreshCatalog(force: Boolean): AppResult<Unit> =
            AppResult.Success(Unit)
    }
}
