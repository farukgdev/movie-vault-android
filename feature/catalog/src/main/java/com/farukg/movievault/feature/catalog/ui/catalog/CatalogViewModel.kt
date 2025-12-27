package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.CatalogRefreshState
import com.farukg.movievault.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CatalogViewModel @Inject constructor(private val repository: CatalogRepository) :
    ViewModel() {

    private val _refreshEvents = MutableSharedFlow<AppError>(extraBufferCapacity = 1)
    val refreshEvents: SharedFlow<AppError> = _refreshEvents.asSharedFlow()

    private val _manualRefreshing = MutableStateFlow(false)
    val manualRefreshing = _manualRefreshing.asStateFlow()

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

    init {
        // Background refresh, don't show snackbars on failure
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            repository.refreshCatalog(force = false)
        }
    }

    // User-initiated refresh: show failures via snackbar events
    fun onUserRefresh() {
        viewModelScope.launch {
            _manualRefreshing.value = true
            when (val result = repository.refreshCatalog(force = true)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> _refreshEvents.tryEmit(result.error)
            }
            _manualRefreshing.value = false
        }
    }

    fun retry() = onUserRefresh()

    // Background refresh, don't show snackbars on failure
    fun onResumed() {
        val now = System.currentTimeMillis()
        if (now - lastResumeRefreshAtEpochMillis < resumeRefreshThrottleMs) return
        lastResumeRefreshAtEpochMillis = now

        // Policy-driven: safe to call often.
        viewModelScope.launch { repository.refreshCatalog(force = false) }
    }
}

private fun Movie.toRowUi(): MovieRowUi {
    val parts = buildList {
        releaseYear?.let { add(it.toString()) }
        rating?.let { add("★ ${"%.1f".format(it)}") }
    }
    val subtitle = if (parts.isEmpty()) "" else parts.joinToString(" • ")
    return MovieRowUi(id = id, title = title, subtitle = subtitle)
}
