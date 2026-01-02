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
import kotlinx.coroutines.flow.collectLatest
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
        val uiLoading: Boolean = false,
        val attemptLoading: Boolean = false,
        val error: AppError? = null,
        val hasItems: Boolean = false,
    )

    private data class RefreshInFlight(
        val origin: RefreshOrigin,
        // we requested a refresh, but mediator may start later
        // don't clear inFlight until attemptLoading=true at least once
        val sawAttemptLoading: Boolean = false,
    )

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
        val hasCache: Boolean? = null,
    ) {
        val isRefreshLoading: Boolean
            get() = paging.attemptLoading

        val isUiLoading: Boolean
            get() = paging.uiLoading

        val hasItems: Boolean
            get() = paging.hasItems

        val isManualRefresh: Boolean
            get() = inFlight?.origin == RefreshOrigin.Manual

        val isBackgroundRefresh: Boolean
            get() = isRefreshLoading && hasItems && !isManualRefresh

        val shouldSpinTopBar: Boolean
            get() = !isManualRefresh && (isUiLoading || (inFlight != null && !hasItems))
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

    val fullScreenError: StateFlow<AppError?> =
        state
            .map { s ->
                val err = s.paging.error

                val show =
                    s.hasCache == false &&
                        !s.paging.hasItems &&
                        !s.everHadItems &&
                        !s.paging.attemptLoading &&
                        s.inFlight == null &&
                        err != null

                if (show) err else null
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
                            hasCache = true,
                        )
                    }
                }
        }

        viewModelScope.launch {
            val has = repository.hasCatalogCache()
            state.update { it.copy(hasCache = has) }
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

    fun retryFromFullScreenError() {
        if (state.value.isRefreshLoading) return
        requestRefresh(RefreshOrigin.Automatic)
    }

    // UI -> VM: called when LazyPagingItems refresh load state / itemCount changes
    fun onPagingRefreshSnapshot(
        uiLoading: Boolean,
        attemptLoading: Boolean,
        error: AppError?,
        hasItems: Boolean,
    ) {
        val prevPaging = state.value.paging
        val isNewError = error != null && error != prevPaging.error
        val origin = state.value.inFlight?.origin ?: RefreshOrigin.Automatic
        val at = if (isNewError) clock.now() else 0L

        state.update { s ->
            val nextPaging =
                PagingRefreshSnapshot(
                    uiLoading = uiLoading,
                    attemptLoading = attemptLoading,
                    error = error,
                    hasItems = hasItems,
                )

            val base =
                s.copy(
                    paging = nextPaging,
                    everHadItems = s.everHadItems || hasItems,
                    hasCache = if (hasItems) true else s.hasCache,
                )

            // attempt lifecycle
            val inflight = base.inFlight
            val newInflight =
                when {
                    inflight == null -> null
                    attemptLoading -> {
                        if (inflight.sawAttemptLoading) inflight
                        else inflight.copy(sawAttemptLoading = true)
                    }
                    inflight.sawAttemptLoading && !attemptLoading -> null // attempt ended
                    else -> inflight // waiting for mediator to start
                }

            // new refresh error
            val newHistory =
                if (isNewError) {
                    base.history.onFailure(error, origin, at)
                } else {
                    base.history
                }

            base.copy(inFlight = newInflight, history = newHistory)
        }

        if (isNewError && origin == RefreshOrigin.Manual) {
            _refreshEvents.tryEmit(error)
        }
    }

    // ----- Internals -----

    private fun requestRefresh(origin: RefreshOrigin) {
        val emitted = _refreshRequests.tryEmit(origin)
        if (emitted) {
            state.update { s -> s.copy(inFlight = RefreshInFlight(origin = origin)) }
        }
    }

    private data class SpinnerSignal(val shouldSpin: Boolean, val showDelayMs: Long)

    private fun CatalogState.spinnerShowDelayMs(): Long {
        val cachePresentOrUnknown = hasCache != false

        return when {
            paging.hasItems -> STATUS_SPINNER_SHOW_DELAY_MS
            cachePresentOrUnknown -> STATUS_SPINNER_SHOW_DELAY_MS
            else -> 0L // no cache
        }
    }

    private fun startTopBarSpinnerController() {
        viewModelScope.launch {
            var shownAt = 0L

            state
                .map { s -> SpinnerSignal(s.shouldSpinTopBar, s.spinnerShowDelayMs()) }
                .distinctUntilChanged()
                .collectLatest { signal ->
                    if (signal.shouldSpin) {
                        if (signal.showDelayMs > 0) delay(signal.showDelayMs)

                        if (state.value.shouldSpinTopBar) {
                            state.update { it.copy(showTopBarSpinner = true) }
                            shownAt = clock.now()
                        }
                    } else {
                        if (state.value.showTopBarSpinner) {
                            val elapsed = clock.now() - shownAt
                            val remaining = STATUS_SPINNER_MIN_VISIBLE_MS - elapsed
                            if (remaining > 0) delay(remaining)
                        }
                        state.update { it.copy(showTopBarSpinner = false) }
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
