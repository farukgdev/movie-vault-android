package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.paging.PagingData
import app.cash.turbine.test
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.feature.catalog.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onResumed emits Automatic refresh request only when stale and not throttled`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = true)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.refreshRequests.test {
                testScheduler.runCurrent()

                vm.onResumed(canAutoRefresh = true)
                testScheduler.runCurrent()
                assertEquals(RefreshOrigin.Automatic, awaitItem())
                assertEquals(1, repo.isStaleCalls)

                // within throttle window
                vm.onResumed(canAutoRefresh = true)
                testScheduler.runCurrent()
                expectNoEvents()
                assertEquals(1, repo.isStaleCalls)

                testScheduler.advanceTimeBy(RESUME_REFRESH_THROTTLE_MS)
                testScheduler.runCurrent()

                vm.onResumed(canAutoRefresh = true)
                testScheduler.runCurrent()
                assertEquals(RefreshOrigin.Automatic, awaitItem())
                assertEquals(2, repo.isStaleCalls)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onResumed does not emit when not stale`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = false)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.refreshRequests.test {
                testScheduler.runCurrent()

                vm.onResumed(canAutoRefresh = true)
                testScheduler.runCurrent()

                expectNoEvents()
                assertEquals(1, repo.isStaleCalls)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ignores refresh requests while paging refresh is loading`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repo = FakeCatalogRepository(stale = true)
            val vm = CatalogViewModel(repo, SchedulerClock(testScheduler))

            vm.refreshRequests.test {
                testScheduler.runCurrent()

                vm.requestManualRefresh()
                testScheduler.runCurrent()
                assertEquals(RefreshOrigin.Manual, awaitItem())

                vm.onPagingRefreshSnapshot(
                    uiLoading = true,
                    attemptLoading = true,
                    error = null,
                    hasItems = true,
                )
                testScheduler.runCurrent()

                // should be ignored while loading
                vm.requestManualRefresh()
                vm.onResumed(canAutoRefresh = true)
                testScheduler.runCurrent()
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock {
        private val baseEpochMillis = 1_700_000_000_000L

        override fun now(): Long = baseEpochMillis + scheduler.currentTime
    }

    private class FakeCatalogRepository(stale: Boolean) : CatalogRepository {
        var staleFlag: Boolean = stale
        var isStaleCalls: Int = 0
            private set

        override fun catalogPaging(): Flow<PagingData<Movie>> = flowOf(PagingData.empty())

        override fun movieDetail(movieId: Long): Flow<AppResult<MovieDetail>> =
            flowOf(AppResult.Error(AppError.Http(404)))

        private val lastUpdated = MutableStateFlow<Long?>(null)

        override fun catalogLastUpdatedEpochMillis(): Flow<Long?> = lastUpdated

        override suspend fun isCatalogStale(nowEpochMillis: Long): Boolean {
            isStaleCalls++
            return staleFlag
        }

        override suspend fun hasCatalogCache(): Boolean = false
    }
}
