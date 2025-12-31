package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val STATUS_SPINNER_SHOW_DELAY_MS = 150L
internal const val STATUS_SPINNER_MIN_VISIBLE_MS = 500L
internal const val RESUME_REFRESH_THROTTLE_MS = 30_000L

@HiltViewModel
class CatalogViewModel
@Inject
constructor(private val repository: CatalogRepository, private val clock: Clock) : ViewModel() {

    data class ScrollPosition(val index: Int = 0, val offset: Int = 0)

    private val _scrollPosition = MutableStateFlow(ScrollPosition())
    val scrollPosition: StateFlow<ScrollPosition> = _scrollPosition.asStateFlow()

    fun onScrollPositionChanged(index: Int, offset: Int) {
        val current = _scrollPosition.value
        if (current.index == index && current.offset == offset) return
        _scrollPosition.value = ScrollPosition(index, offset)
    }

    // VM -> UI: call items.refresh()
    private val _refreshRequests = MutableSharedFlow<RefreshOrigin>(extraBufferCapacity = 1)
    val refreshRequests: SharedFlow<RefreshOrigin> = _refreshRequests.asSharedFlow()

    // VM -> UI: snackbar for manual refresh failures
    private val _refreshEvents = MutableSharedFlow<AppError>(extraBufferCapacity = 1)
    val refreshEvents: SharedFlow<AppError> = _refreshEvents.asSharedFlow()

    val moviesPaging: Flow<PagingData<MovieRowUi>> =
        repository
            .catalogPaging()
            .map { pagingData -> pagingData.map { it.toRowUi() } }
            .cachedIn(viewModelScope)

    // ----- Internal state model -----

    private data class PagingRefreshSnapshot(
        val isLoading: Boolean = false,
        val error: AppError? = null,
        val hasItems: Boolean = false,
    )

    private data class RefreshInFlight(val origin: RefreshOrigin)

    private data class LastFailure(val error: AppError, val origin: RefreshOrigin, val at: Long)

    private data class RefreshHistory(
        val lastSuccessAt: Long? = null,
        val lastFailure: LastFailure? = null,
    ) {
        fun onSuccess(at: Long): RefreshHistory = copy(lastSuccessAt = at, lastFailure = null)

        fun onFailure(error: AppError, origin: RefreshOrigin, at: Long): RefreshHistory {
            val newFailure = LastFailure(error = error, origin = origin, at = at)
            val keepOld = lastFailure?.at?.let { it > at } == true
            return if (keepOld) this else copy(lastFailure = newFailure)
        }

        fun currentFailure(): LastFailure? {
            val lastSuccess = lastSuccessAt ?: Long.MIN_VALUE
            return lastFailure?.takeIf { it.at > lastSuccess }
        }
    }

    private data class CatalogState(
        val lastUpdatedEpochMillis: Long? = null,
        val paging: PagingRefreshSnapshot = PagingRefreshSnapshot(),
        val inFlight: RefreshInFlight? = null,
        val history: RefreshHistory = RefreshHistory(),
        val showTopBarSpinner: Boolean = false,
        val everHadItems: Boolean = false,
        val isStale: Boolean = false,
        val autoRefreshDeferred: Boolean = false,
    ) {
        val isRefreshLoading: Boolean
            get() = paging.isLoading

        val hasItems: Boolean
            get() = paging.hasItems

        val isManualRefresh: Boolean
            get() = inFlight?.origin == RefreshOrigin.Manual

        val isBackgroundRefresh: Boolean
            get() = isRefreshLoading && hasItems && !isManualRefresh
    }

    private val state = MutableStateFlow(CatalogState())

    val manualRefreshing: StateFlow<Boolean> =
        state
            .map { it.isRefreshLoading && it.isManualRefresh }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val everHadItems: StateFlow<Boolean> =
        state
            .map { it.everHadItems }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val statusUi: StateFlow<CatalogStatusUi> =
        state
            .map { reduceStatusUi(it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CatalogStatusUi(
                    lastUpdatedEpochMillis = null,
                    icon = CatalogStatusIcon.Ok,
                    error = null,
                    errorOrigin = null,
                    isRefreshing = false,
                ),
            )

    private var lastResumeRefreshAtEpochMillis: Long = 0L

    init {
        viewModelScope.launch {
            repository
                .catalogLastUpdatedEpochMillis()
                .filterNotNull()
                .distinctUntilChanged()
                .collect { at ->
                    state.update { s ->
                        s.copy(
                            lastUpdatedEpochMillis = at,
                            history = s.history.onSuccess(at),
                            isStale = false,
                            autoRefreshDeferred = false,
                        )
                    }
                }
        }

        startTopBarSpinnerController()
    }

    // ----- Public API -----

    // conditional auto-refresh
    fun onResumed(canAutoRefresh: Boolean) {
        val now = clock.now()
        if (now - lastResumeRefreshAtEpochMillis < RESUME_REFRESH_THROTTLE_MS) return
        lastResumeRefreshAtEpochMillis = now

        viewModelScope.launch {
            if (state.value.isRefreshLoading) return@launch

            val stale = repository.isCatalogStale(now)

            state.update { s ->
                s.copy(isStale = stale, autoRefreshDeferred = stale && !canAutoRefresh)
            }

            if (stale && canAutoRefresh) {
                requestRefresh(RefreshOrigin.Automatic)
            }
        }
    }

    fun requestManualRefresh() {
        if (state.value.isRefreshLoading) return
        state.update { s -> s.copy(autoRefreshDeferred = false) }
        requestRefresh(RefreshOrigin.Manual)
    }

    // UI -> VM: called when LazyPagingItems refresh load state / itemCount changes
    fun onPagingRefreshSnapshot(isLoading: Boolean, error: AppError?, hasItems: Boolean) {
        val prev = state.value.paging
        val next = PagingRefreshSnapshot(isLoading = isLoading, error = error, hasItems = hasItems)

        state.update { s -> s.copy(paging = next, everHadItems = s.everHadItems || hasItems) }

        // new refresh error
        if (error != null && error != prev.error) {
            val origin = state.value.inFlight?.origin ?: RefreshOrigin.Automatic
            val at = clock.now()

            state.update { s -> s.copy(history = s.history.onFailure(error, origin, at)) }

            if (origin == RefreshOrigin.Manual) {
                _refreshEvents.tryEmit(error)
            }
        }

        // refresh finished (success or error)
        if (prev.isLoading && !isLoading) {
            state.update { s -> s.copy(inFlight = null) }
        }
    }

    // ----- Internals -----

    private fun requestRefresh(origin: RefreshOrigin) {
        val emitted = _refreshRequests.tryEmit(origin)
        if (emitted) {
            state.update { s -> s.copy(inFlight = RefreshInFlight(origin)) }
        }
    }

    private fun startTopBarSpinnerController() {
        viewModelScope.launch {
            var shownAt = 0L

            state
                .map { it.isBackgroundRefresh }
                .distinctUntilChanged()
                .collect { shouldSpin ->
                    if (shouldSpin) {
                        delay(STATUS_SPINNER_SHOW_DELAY_MS)
                        if (state.value.isBackgroundRefresh) {
                            state.update { s -> s.copy(showTopBarSpinner = true) }
                            shownAt = clock.now()
                        }
                    } else {
                        if (state.value.showTopBarSpinner) {
                            val elapsed = clock.now() - shownAt
                            val remaining = STATUS_SPINNER_MIN_VISIBLE_MS - elapsed
                            if (remaining > 0) delay(remaining)
                        }
                        state.update { s -> s.copy(showTopBarSpinner = false) }
                    }
                }
        }
    }

    private fun reduceStatusUi(s: CatalogState): CatalogStatusUi {
        val failure = s.history.currentFailure()

        val icon =
            when {
                s.showTopBarSpinner -> CatalogStatusIcon.BackgroundRefreshing
                failure?.error is AppError.Offline -> CatalogStatusIcon.Offline
                failure != null -> CatalogStatusIcon.Error
                s.autoRefreshDeferred || s.isStale -> CatalogStatusIcon.Stale
                else -> CatalogStatusIcon.Ok
            }

        return CatalogStatusUi(
            lastUpdatedEpochMillis = s.lastUpdatedEpochMillis,
            icon = icon,
            error = failure?.error,
            errorOrigin = failure?.origin,
            isRefreshing = s.isRefreshLoading,
        )
    }
}

enum class RefreshOrigin {
    Automatic,
    Manual,
}

enum class CatalogStatusIcon {
    Ok,
    Stale,
    Offline,
    Error,
    BackgroundRefreshing,
}

data class CatalogStatusUi(
    val lastUpdatedEpochMillis: Long?,
    val icon: CatalogStatusIcon,
    val error: AppError?,
    val errorOrigin: RefreshOrigin?,
    val isRefreshing: Boolean,
)

private fun Movie.toRowUi(): MovieRowUi =
    MovieRowUi(
        id = id,
        title = title,
        releaseYear = releaseYear,
        rating = rating,
        posterUrl = posterUrl,
    )
