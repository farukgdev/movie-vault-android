package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.paging.PagingData
import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.data.repository.MovieDetailCacheState
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
    fun `manual refresh failure emits refreshEvents and sets Manual origin`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = false)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            turbineScope {
                val statusTurbine = vm.statusUi.testIn(this)
                val requestsTurbine = vm.refreshRequests.testIn(this)
                val eventsTurbine = vm.refreshEvents.testIn(this)

                testScheduler.runCurrent()

                statusTurbine.awaitItem()

                vm.requestManualRefresh()
                testScheduler.runCurrent()
                assertEquals(RefreshOrigin.Manual, requestsTurbine.awaitItem())

                vm.onPagingRefreshSnapshot(
                    uiLoading = true,
                    attemptLoading = true,
                    error = null,
                    hasItems = true,
                )
                testScheduler.runCurrent()

                val err = AppError.Network()
                vm.onPagingRefreshSnapshot(
                    uiLoading = false,
                    attemptLoading = false,
                    error = err,
                    hasItems = true,
                )
                testScheduler.runCurrent()

                assertEquals(err, eventsTurbine.awaitItem())

                assertEquals(CatalogStatusIcon.Error, vm.statusUi.value.icon)
                assertEquals(err, vm.statusUi.value.error)
                assertEquals(RefreshOrigin.Manual, vm.statusUi.value.errorOrigin)

                statusTurbine.cancelAndIgnoreRemainingEvents()
                requestsTurbine.cancelAndIgnoreRemainingEvents()
                eventsTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `automatic refresh failure sets Offline icon and Automatic origin`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = true)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                awaitItem()

                vm.refreshRequests.test {
                    vm.onResumed(canAutoRefresh = true)
                    testScheduler.advanceUntilIdle()

                    assertEquals(RefreshOrigin.Automatic, awaitItem())

                    vm.onPagingRefreshSnapshot(
                        uiLoading = true,
                        attemptLoading = true,
                        error = null,
                        hasItems = false,
                    )
                    testScheduler.runCurrent()

                    vm.onPagingRefreshSnapshot(
                        uiLoading = false,
                        attemptLoading = false,
                        error = AppError.Offline(),
                        hasItems = false,
                    )
                    testScheduler.runCurrent()

                    assertEquals(CatalogStatusIcon.Offline, vm.statusUi.value.icon)
                    assertTrue(vm.statusUi.value.error is AppError.Offline)
                    assertEquals(RefreshOrigin.Automatic, vm.statusUi.value.errorOrigin)

                    cancelAndIgnoreRemainingEvents()
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `success signal clears previous error`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = true)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                awaitItem()

                vm.onPagingRefreshSnapshot(
                    uiLoading = false,
                    attemptLoading = false,
                    error = AppError.Network(),
                    hasItems = false,
                )
                testScheduler.runCurrent()

                assertEquals(CatalogStatusIcon.Error, vm.statusUi.value.icon)
                assertTrue(vm.statusUi.value.error is AppError.Network)

                repo.lastUpdatedFlow.value = 123L
                testScheduler.advanceUntilIdle()

                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)
                assertNull(vm.statusUi.value.error)
                assertNull(vm.statusUi.value.errorOrigin)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `background spinner shows after delay and stays for min duration`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = false)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                awaitItem()

                vm.onPagingRefreshSnapshot(
                    uiLoading = true,
                    attemptLoading = true,
                    error = null,
                    hasItems = true,
                )
                testScheduler.runCurrent()

                testScheduler.advanceTimeBy(STATUS_SPINNER_SHOW_DELAY_MS - 1)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)

                testScheduler.advanceTimeBy(1)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.BackgroundRefreshing, vm.statusUi.value.icon)

                vm.onPagingRefreshSnapshot(
                    uiLoading = false,
                    attemptLoading = false,
                    error = null,
                    hasItems = true,
                )
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
            val repo = FakeCatalogRepository(stale = false)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.statusUi.test {
                awaitItem()

                vm.onPagingRefreshSnapshot(
                    uiLoading = true,
                    attemptLoading = true,
                    error = null,
                    hasItems = true,
                )
                testScheduler.runCurrent()

                testScheduler.advanceTimeBy(STATUS_SPINNER_SHOW_DELAY_MS - 1)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)

                vm.onPagingRefreshSnapshot(
                    uiLoading = false,
                    attemptLoading = false,
                    error = null,
                    hasItems = true,
                )

                testScheduler.advanceTimeBy(2)
                testScheduler.runCurrent()
                assertEquals(CatalogStatusIcon.Ok, vm.statusUi.value.icon)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `fullScreenError retry clears blocking error immediately and emits Automatic refresh request`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = false)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            turbineScope {
                val fullScreenTurbine = vm.fullScreenError.testIn(this)
                val requestsTurbine = vm.refreshRequests.testIn(this)

                testScheduler.runCurrent()

                assertNull(fullScreenTurbine.awaitItem())

                val err = AppError.Offline()
                vm.onPagingRefreshSnapshot(
                    uiLoading = false,
                    attemptLoading = false,
                    error = err,
                    hasItems = false,
                )
                testScheduler.runCurrent()

                assertEquals(err, fullScreenTurbine.awaitItem())

                vm.retryFromFullScreenError()
                testScheduler.runCurrent()

                assertEquals(RefreshOrigin.Automatic, requestsTurbine.awaitItem())
                // it should immediately clear the error, so UI can show loading state
                assertNull(fullScreenTurbine.awaitItem())

                fullScreenTurbine.cancelAndIgnoreRemainingEvents()
                requestsTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock {
        private val baseEpochMillis = 1_700_000_000_000L

        override fun now(): Long = baseEpochMillis + scheduler.currentTime
    }

    private class FakeCatalogRepository(stale: Boolean) : CatalogRepository {
        var staleFlag: Boolean = stale
        var hasCacheFlag: Boolean = false
        val lastUpdatedFlow = MutableStateFlow<Long?>(null)

        override fun catalogLastUpdatedEpochMillis(): Flow<Long?> = lastUpdatedFlow

        override fun catalogPaging(): Flow<PagingData<Movie>> = flowOf(PagingData.empty())

        override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> =
            flowOf(AppResult.Error(AppError.Http(404)))

        override suspend fun refreshMovieDetail(movieId: Long): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun movieDetailCacheState(movieId: Long): MovieDetailCacheState =
            MovieDetailCacheState.Missing

        override suspend fun isCatalogStale(nowEpochMillis: Long): Boolean = staleFlag

        override suspend fun hasCatalogCache(): Boolean = hasCacheFlag
    }
}
