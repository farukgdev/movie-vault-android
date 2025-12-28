package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.CatalogRefreshState
import com.farukg.movievault.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal const val STATUS_SPINNER_SHOW_DELAY_MS = 150L
internal const val STATUS_SPINNER_MIN_VISIBLE_MS = 500L

@HiltViewModel
class CatalogViewModel
@Inject
constructor(private val repository: CatalogRepository, private val clock: Clock) : ViewModel() {

    private val _refreshEvents = MutableSharedFlow<AppError>(extraBufferCapacity = 1)
    val refreshEvents: SharedFlow<AppError> = _refreshEvents.asSharedFlow()

    private val _manualRefreshing = MutableStateFlow(false)
    val manualRefreshing: StateFlow<Boolean> = _manualRefreshing.asStateFlow()

    private data class LastFailure(val error: AppError, val origin: RefreshOrigin, val at: Long)

    private data class RefreshLog(
        val lastSuccessAt: Long? = null,
        val lastFailure: LastFailure? = null,
    )

    private val _refreshLog = MutableStateFlow(RefreshLog())

    private val _showBackgroundSpinnerInTopBar = MutableStateFlow(false)
    private val showBackgroundSpinnerInTopBar: StateFlow<Boolean> =
        _showBackgroundSpinnerInTopBar.asStateFlow()

    private val bgMutex = Mutex()

    private var lastResumeRefreshAtEpochMillis: Long = 0L
    private val resumeRefreshThrottleMs: Long = 30_000L

    val uiState: StateFlow<CatalogUiState> =
        repository
            .catalog()
            .map { result ->
                when (result) {
                    is AppResult.Error -> CatalogUiState.Error(result.error)
                    is AppResult.Success -> {
                        val rows = result.data.map { it.toRowUi() }
                        if (rows.isEmpty()) CatalogUiState.Empty else CatalogUiState.Content(rows)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState.Loading)

    val refreshState: StateFlow<CatalogRefreshState> =
        repository
            .catalogRefreshState()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CatalogRefreshState(
                    lastUpdatedEpochMillis = null,
                    isRefreshing = false,
                    lastRefreshError = null,
                ),
            )

    val statusUi: StateFlow<CatalogStatusUi> =
        combine(refreshState, _refreshLog, showBackgroundSpinnerInTopBar, manualRefreshing) {
                rs,
                log,
                showBgSpinner,
                isManualRefreshing ->
                buildCatalogStatusUi(
                    refreshState = rs,
                    log = log,
                    showBackgroundSpinnerInTopBar = showBgSpinner,
                    isManualRefreshing = isManualRefreshing,
                )
            }
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

    init {
        startTopBarSpinnerController()
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { runBackgroundRefresh() }
    }

    fun onResumed() {
        val now = clock.now()
        if (now - lastResumeRefreshAtEpochMillis < resumeRefreshThrottleMs) return
        lastResumeRefreshAtEpochMillis = now
        viewModelScope.launch { runBackgroundRefresh() }
    }

    fun onUserRefresh() {
        viewModelScope.launch {
            _manualRefreshing.value = true
            _showBackgroundSpinnerInTopBar.value = false

            val result = repository.refreshCatalog(force = true)
            val at = clock.now()

            when (result) {
                is AppResult.Success -> {
                    recordSuccess(at)
                }
                is AppResult.Error -> {
                    recordFailure(result.error, RefreshOrigin.Manual, at)
                    _refreshEvents.tryEmit(result.error)
                }
            }

            _manualRefreshing.value = false
        }
    }

    fun retry() = onUserRefresh()

    private fun startTopBarSpinnerController() {
        // don't show immediately (avoid flicker)
        val showDelayMs = STATUS_SPINNER_SHOW_DELAY_MS
        val minVisibleMs = STATUS_SPINNER_MIN_VISIBLE_MS

        viewModelScope.launch {
            var shownAt = 0L

            combine(refreshState, manualRefreshing) { rs, manual ->
                    // only background refresh is eligible for top-bar spinner.
                    rs.isRefreshing && !manual
                }
                .distinctUntilChanged()
                .collectLatest { shouldSpin ->
                    if (shouldSpin) {
                        delay(showDelayMs)

                        if (refreshState.value.isRefreshing && !manualRefreshing.value) {
                            _showBackgroundSpinnerInTopBar.value = true
                            shownAt = clock.now()
                        }
                    } else {
                        if (_showBackgroundSpinnerInTopBar.value) {
                            val elapsed = clock.now() - shownAt
                            val remaining = minVisibleMs - elapsed
                            if (remaining > 0) delay(remaining)
                        }
                        _showBackgroundSpinnerInTopBar.value = false
                    }
                }
        }
    }

    private suspend fun runBackgroundRefresh() {
        if (!bgMutex.tryLock()) return
        try {
            val result = repository.refreshCatalog(force = false)
            val at = clock.now()

            when (result) {
                is AppResult.Success -> recordSuccess(at)
                is AppResult.Error -> recordFailure(result.error, RefreshOrigin.Automatic, at)
            }
        } finally {
            bgMutex.unlock()
        }
    }

    private fun buildCatalogStatusUi(
        refreshState: CatalogRefreshState,
        log: RefreshLog,
        showBackgroundSpinnerInTopBar: Boolean,
        isManualRefreshing: Boolean,
    ): CatalogStatusUi {
        val isRefreshing = isManualRefreshing || refreshState.isRefreshing

        val showSpinnerInIcon = !isManualRefreshing && showBackgroundSpinnerInTopBar

        val lastSuccessAt = log.lastSuccessAt ?: Long.MIN_VALUE
        val failure = log.lastFailure?.takeIf { it.at > lastSuccessAt }

        val icon =
            when {
                showSpinnerInIcon -> CatalogStatusIcon.BackgroundRefreshing
                failure?.error is AppError.Offline -> CatalogStatusIcon.Offline
                failure != null -> CatalogStatusIcon.Error
                else -> CatalogStatusIcon.Ok
            }

        return CatalogStatusUi(
            lastUpdatedEpochMillis = refreshState.lastUpdatedEpochMillis,
            icon = icon,
            error = failure?.error,
            errorOrigin = failure?.origin,
            isRefreshing = isRefreshing,
        )
    }

    private fun recordSuccess(at: Long) {
        _refreshLog.update { it.copy(lastSuccessAt = at, lastFailure = null) }
    }

    private fun recordFailure(error: AppError, origin: RefreshOrigin, at: Long) {
        _refreshLog.update { current ->
            val newFailure = LastFailure(error = error, origin = origin, at = at)

            // keep the newest failure only
            val keepOld = current.lastFailure?.at?.let { it > at } == true
            if (keepOld) current else current.copy(lastFailure = newFailure)
        }
    }
}

enum class RefreshOrigin {
    Automatic,
    Manual,
}

enum class CatalogStatusIcon {
    Ok,
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

private fun Movie.toRowUi(): MovieRowUi {
    val parts = buildList {
        releaseYear?.let { add(it.toString()) }
        rating?.let { add("★ ${"%.1f".format(it)}") }
    }
    val subtitle = if (parts.isEmpty()) "" else parts.joinToString(" • ")
    return MovieRowUi(id = id, title = title, subtitle = subtitle)
}
