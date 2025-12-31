package com.farukg.movievault.feature.catalog.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.remote.tmdb.TmdbImageSize
import com.farukg.movievault.data.remote.tmdb.tmdbWithSizeOrNull
import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel
@Inject
constructor(
    private val catalogRepository: CatalogRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val movieId = MutableStateFlow<Long?>(null)

    // uiState starts as Loading and won't emit real content until given a non-null id
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> =
        movieId
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { id -> catalogRepository.movieDetail(id) }
            .map { result ->
                when (result) {
                    is AppResult.Error -> DetailUiState.Error(result.error)
                    is AppResult.Success -> result.data.toUiState()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    fun setMovieId(id: Long) {
        movieId.value = id
    }

    fun retry() {
        // For now no-op
    }

    fun toggleFavorite() {
        val id = movieId.value ?: return
        viewModelScope.launch { favoritesRepository.toggleFavorite(id) }
    }
}

private fun MovieDetail.toUiState(): DetailUiState.Content {
    val metaPrimary =
        buildList {
                if (genres.isNotEmpty()) add(genres.take(2).joinToString(", "))
                releaseYear?.let { add(it.toString()) }
            }
            .joinToString(" • ")

    val metaSecondary =
        buildList {
                rating?.let { add("★ ${"%.1f".format(it)}") }
                runtimeMinutes?.let { add("${it}m") }
            }
            .joinToString(" • ")

    return DetailUiState.Content(
        title = title,
        metaPrimary = metaPrimary,
        metaSecondary = metaSecondary,
        overview = overview.ifBlank { "No overview available." },
        posterUrl = posterUrl,
        isFavorite = isFavorite,
        posterFallbackUrl = posterUrl.tmdbWithSizeOrNull(TmdbImageSize.List),
    )
}
