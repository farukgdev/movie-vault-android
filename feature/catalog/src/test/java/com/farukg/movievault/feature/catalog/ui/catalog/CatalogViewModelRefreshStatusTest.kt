package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.paging.PagingData
import app.cash.turbine.test
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.repository.CatalogRefreshState
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.feature.catalog.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelRefreshStatusTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial background refresh failure sets Offline icon and Automatic origin`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                FakeCatalogRepository().apply {
                    enqueueRefreshResult(AppResult.Error(AppError.Offline()))
                }

            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                testScheduler.advanceUntilIdle()

                assertEquals(listOf(false), repo.refreshForceCalls)

                assertEquals(CatalogStatusIcon.Offline, vm.statusUi.value.icon)
                assertTrue(vm.statusUi.value.error is AppError.Offline)
                assertEquals(RefreshOrigin.Automatic, vm.statusUi.value.errorOrigin)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `success after failure clears current error and returns to Ok`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                FakeCatalogRepository().apply {
                    enqueueRefreshResult(AppResult.Error(AppError.Network()))
                }

            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                testScheduler.advanceUntilIdle()

                assertEquals(CatalogStatusIcon.Error, vm.statusUi.value.icon)
                assertTrue(vm.statusUi.value.error is AppError.Network)
                assertEquals(RefreshOrigin.Automatic, vm.statusUi.value.errorOrigin)

                repo.enqueueRefreshResult(AppResult.Success(Unit))
                vm.onResumed()
                testScheduler.advanceUntilIdle()

                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)
                assertNull(vm.statusUi.value.error)
                assertNull(vm.statusUi.value.errorOrigin)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `manual refresh failure emits refresh event and sets Error icon with Manual origin`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                FakeCatalogRepository().apply { enqueueRefreshResult(AppResult.Success(Unit)) }

            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                vm.refreshEvents.test {
                    testScheduler.advanceUntilIdle()

                    repo.enqueueRefreshResult(AppResult.Error(AppError.Network()))

                    vm.onUserRefresh()
                    testScheduler.advanceUntilIdle()

                    val emitted = awaitItem()
                    assertTrue(emitted is AppError.Network)

                    assertEquals(CatalogStatusIcon.Error, vm.statusUi.value.icon)
                    assertTrue(vm.statusUi.value.error is AppError.Network)
                    assertEquals(RefreshOrigin.Manual, vm.statusUi.value.errorOrigin)

                    assertTrue(repo.refreshForceCalls.contains(true)) // manual

                    cancelAndIgnoreRemainingEvents()
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `background spinner shows only after delay and stays visible for min duration`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                FakeCatalogRepository().apply { enqueueRefreshResult(AppResult.Success(Unit)) }

            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                testScheduler.advanceUntilIdle()

                repo.refreshStateFlow.value =
                    repo.refreshStateFlow.value.copy(isRefreshing = true, lastRefreshError = null)
                testScheduler.runCurrent()

                // before 150ms delay: icon is not spinner
                testScheduler.advanceTimeBy(STATUS_SPINNER_SHOW_DELAY_MS - 1)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)

                // after 150ms icon should be BackgroundRefreshing
                testScheduler.advanceTimeBy(1)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.BackgroundRefreshing, vm.statusUi.value.icon)

                // stop refreshing, icon should remain for minVisibleMs
                repo.refreshStateFlow.value = repo.refreshStateFlow.value.copy(isRefreshing = false)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.BackgroundRefreshing, vm.statusUi.value.icon)

                testScheduler.advanceTimeBy(STATUS_SPINNER_MIN_VISIBLE_MS - 1)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.BackgroundRefreshing, vm.statusUi.value.icon)

                testScheduler.advanceTimeBy(1)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `background spinner never shows if refresh ends before delay`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                FakeCatalogRepository().apply { enqueueRefreshResult(AppResult.Success(Unit)) }

            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                testScheduler.advanceUntilIdle()

                repo.refreshStateFlow.value = repo.refreshStateFlow.value.copy(isRefreshing = true)
                testScheduler.runCurrent()

                testScheduler.advanceTimeBy(STATUS_SPINNER_SHOW_DELAY_MS - 1)
                repo.refreshStateFlow.value = repo.refreshStateFlow.value.copy(isRefreshing = false)
                testScheduler.advanceUntilIdle()

                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onResumed is throttled within 30 seconds`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo =
                FakeCatalogRepository().apply { enqueueRefreshResult(AppResult.Success(Unit)) }

            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                testScheduler.advanceUntilIdle()
                assertEquals(1, repo.refreshCallCount)

                // first resume should refresh
                repo.enqueueRefreshResult(AppResult.Success(Unit))
                vm.onResumed()
                testScheduler.advanceUntilIdle()
                assertEquals(2, repo.refreshCallCount)

                // resume again (< 30s) should be ignored
                repo.enqueueRefreshResult(AppResult.Success(Unit))
                vm.onResumed()
                testScheduler.advanceUntilIdle()
                assertEquals(2, repo.refreshCallCount)

                // after 30s should refresh again
                testScheduler.advanceTimeBy(30_000)
                repo.enqueueRefreshResult(AppResult.Success(Unit))
                vm.onResumed()
                testScheduler.advanceUntilIdle()
                assertEquals(3, repo.refreshCallCount)

                cancelAndIgnoreRemainingEvents()
            }
        }

    private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock {
        private val baseEpochMillis = 1_700_000_000_000L

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun now(): Long = baseEpochMillis + scheduler.currentTime
    }

    private class FakeCatalogRepository : CatalogRepository {
        private val catalogFlow =
            MutableStateFlow<AppResult<List<Movie>>>(AppResult.Success(emptyList()))

        val refreshStateFlow =
            MutableStateFlow(
                CatalogRefreshState(
                    lastUpdatedEpochMillis = null,
                    isRefreshing = false,
                    lastRefreshError = null,
                )
            )

        val refreshForceCalls = mutableListOf<Boolean>()
        private val refreshResults = ArrayDeque<AppResult<Unit>>()

        var refreshCallCount: Int = 0
            private set

        fun enqueueRefreshResult(result: AppResult<Unit>) {
            refreshResults.addLast(result)
        }

        override fun catalog(): Flow<AppResult<List<Movie>>> = catalogFlow

        override fun catalogPaging(): Flow<PagingData<Movie>> = flowOf(PagingData.empty())

        override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> =
            flowOf(AppResult.Error(AppError.Http(404)))

        override fun catalogRefreshState(): Flow<CatalogRefreshState> = refreshStateFlow

        override suspend fun refreshCatalog(force: Boolean): AppResult<Unit> {
            refreshCallCount++
            refreshForceCalls += force
            return if (refreshResults.isEmpty()) AppResult.Success(Unit)
            else refreshResults.removeFirst()
        }
    }
}
