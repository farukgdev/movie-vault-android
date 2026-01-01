package com.farukg.movievault.feature.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(private val repository: FavoritesRepository) :
    ViewModel() {
    private val ioScope =
        kotlinx.coroutines.CoroutineScope(viewModelScope.coroutineContext + Dispatchers.IO)

    val uiState: StateFlow<FavoritesUiState> =
        repository
            .favorites()
            .map { result ->
                when (result) {
                    is AppResult.Error -> FavoritesUiState.Error(result.error)
                    is AppResult.Success -> {
                        val rows = result.data.map { it.toRowUi() }
                        if (rows.isEmpty()) FavoritesUiState.Empty
                        else FavoritesUiState.Content(rows)
                    }
                }
            }
            .stateIn(ioScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState.Loading)

    fun retry() {
        // For now no-op
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }
}

private fun Movie.toRowUi(): FavoriteRowUi =
    FavoriteRowUi(
        id = id,
        title = title,
        releaseYear = releaseYear,
        rating = rating,
        posterUrl = posterUrl,
    )
