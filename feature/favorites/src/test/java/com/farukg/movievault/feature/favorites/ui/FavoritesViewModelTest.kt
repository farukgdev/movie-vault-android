package com.farukg.movievault.feature.favorites.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.FavoritesRepository
import com.farukg.movievault.feature.favorites.testing.MainDispatcherRule
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {

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
    fun `maps success empty to Empty`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val repo = TestFavoritesRepository(initial = AppResult.Success(emptyList()))
            val vm = FavoritesViewModel(repo)

            vm.uiState.test {
                val state = awaitSettled(scheduler)
                assertEquals(FavoritesUiState.Empty, state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `maps success non-empty to Content with mapped rows`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val repo =
                TestFavoritesRepository(
                    initial =
                        AppResult.Success(
                            listOf(
                                Movie(id = 1, title = "A", releaseYear = 2024, rating = null),
                                Movie(id = 2, title = "B", releaseYear = null, rating = null),
                            )
                        )
                )
            val vm = FavoritesViewModel(repo)

            vm.uiState.test {
                val state = awaitSettled(scheduler)
                assertEquals(
                    FavoritesUiState.Content(
                        movies =
                            listOf(
                                FavoriteRowUi(id = 1, title = "A", subtitle = "2024"),
                                FavoriteRowUi(id = 2, title = "B", subtitle = ""),
                            )
                    ),
                    state,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `maps error to Error with same AppError`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val err = AppError.Network()
            val repo = TestFavoritesRepository(initial = AppResult.Error(err))
            val vm = FavoritesViewModel(repo)

            vm.uiState.test {
                val state = awaitSettled(scheduler)
                assertTrue(state is FavoritesUiState.Error)
                assertEquals(err, (state as FavoritesUiState.Error).error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `uiState updates when repository emits new values`() =
        runTest(mainDispatcherRule.dispatcher) {
            val scheduler = testScheduler
            val repo = TestFavoritesRepository(initial = AppResult.Success(emptyList()))
            val vm = FavoritesViewModel(repo)

            vm.uiState.test {
                assertEquals(FavoritesUiState.Empty, awaitSettled(scheduler))

                repo.emit(
                    AppResult.Success(
                        listOf(Movie(id = 3, title = "C", releaseYear = 2022, rating = null))
                    )
                )
                scheduler.runCurrent()

                assertEquals(
                    FavoritesUiState.Content(
                        movies = listOf(FavoriteRowUi(id = 3, title = "C", subtitle = "2022"))
                    ),
                    awaitItem(),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- Helpers

    private suspend fun ReceiveTurbine<FavoritesUiState>.awaitSettled(
        scheduler: TestCoroutineScheduler
    ): FavoritesUiState {
        val first = awaitItem()
        if (first != FavoritesUiState.Loading) return first
        scheduler.advanceUntilIdle()
        return awaitItem()
    }

    private class TestFavoritesRepository(initial: AppResult<List<Movie>>) : FavoritesRepository {
        private val favoritesFlow = MutableStateFlow(initial)

        fun emit(value: AppResult<List<Movie>>) {
            favoritesFlow.value = value
        }

        override fun favorites(): Flow<AppResult<List<Movie>>> = favoritesFlow

        override suspend fun toggleFavorite(movieId: Long): AppResult<Boolean> =
            AppResult.Success(true)
    }
}
