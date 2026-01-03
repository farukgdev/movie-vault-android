package com.farukg.movievault.feature.catalog.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.remote.tmdb.TmdbImageSize
import com.farukg.movievault.data.remote.tmdb.tmdbWithSizeOrNull
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.data.repository.FavoritesRepository
import com.farukg.movievault.data.repository.MovieDetailCacheState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val MIN_REFRESH_VISIBLE_MS = 500L

@HiltViewModel
class DetailViewModel
@Inject
constructor(
    private val catalogRepository: CatalogRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val movieId = MutableStateFlow<Long?>(null)

    private data class RefreshState(
        val isRefreshing: Boolean = false,
        val stickyError: AppError? = null,
        val hasAttempted: Boolean = false,
    )

    private val refreshState = MutableStateFlow(RefreshState())
    private var refreshJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val detailResultFlow =
        movieId.filterNotNull().distinctUntilChanged().flatMapLatest { id ->
            catalogRepository.movieDetail(id)
        }

    val uiState: StateFlow<DetailUiState> =
        combine(detailResultFlow, refreshState) { result, refresh ->
                when (result) {
                    is AppResult.Success -> {
                        val d = result.data
                        val hasFetchedDetail = d.detailFetchedAtEpochMillis != null

                        DetailUiState.Content(
                            title = d.title,
                            genres = d.genres.take(6),
                            releaseYear = d.releaseYear,
                            rating = d.rating,
                            runtimeMinutes = d.runtimeMinutes,
                            overview = d.overview,
                            posterUrl = d.posterUrl,
                            posterFallbackUrl = d.posterUrl.tmdbWithSizeOrNull(TmdbImageSize.List),
                            isFavorite = d.isFavorite,
                            hasFetchedDetail = hasFetchedDetail,
                            isRefreshing = refresh.isRefreshing,
                            bannerError = refresh.stickyError,
                            hasAttemptedRefresh = refresh.hasAttempted,
                        )
                    }

                    is AppResult.Error -> {
                        if (!refresh.hasAttempted || refresh.isRefreshing) {
                            DetailUiState.Loading(isRefreshing = refresh.isRefreshing)
                        } else {
                            DetailUiState.NoCacheError(error = refresh.stickyError ?: result.error)
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading())

    fun setMovieId(id: Long) {
        if (movieId.value == id) return

        movieId.value = id
        refreshJob?.cancel()
        refreshState.value = RefreshState()

        viewModelScope.launch {
            val state = catalogRepository.movieDetailCacheState(id)
            if (state != MovieDetailCacheState.Fetched) {
                refresh()
            }
        }
    }

    fun refresh() {
        val id = movieId.value ?: return
        if (refreshState.value.isRefreshing) return

        refreshState.update { it.copy(isRefreshing = true, hasAttempted = true) }

        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                val startedAt = System.currentTimeMillis()

                val result = catalogRepository.refreshMovieDetail(id)

                val elapsed = System.currentTimeMillis() - startedAt
                val remaining = MIN_REFRESH_VISIBLE_MS - elapsed
                if (remaining > 0) delay(remaining)

                when (result) {
                    is AppResult.Success -> {
                        refreshState.update {
                            it.copy(isRefreshing = false, stickyError = null, hasAttempted = true)
                        }
                    }
                    is AppResult.Error -> {
                        refreshState.update {
                            it.copy(
                                isRefreshing = false,
                                stickyError = result.error,
                                hasAttempted = true,
                            )
                        }
                    }
                }
            }
    }

    fun toggleFavorite() {
        val id = movieId.value ?: return
        viewModelScope.launch { favoritesRepository.toggleFavorite(id) }
    }
}
