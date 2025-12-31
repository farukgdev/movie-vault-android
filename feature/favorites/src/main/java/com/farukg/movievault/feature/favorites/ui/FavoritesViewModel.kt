package com.farukg.movievault.feature.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FavoritesViewModel @Inject constructor(repository: FavoritesRepository) : ViewModel() {
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
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                FavoritesUiState.Loading,
            )

    fun retry() {
        // For now no-op
    }
}

private fun Movie.toRowUi(): FavoriteRowUi =
    FavoriteRowUi(id = id, title = title, releaseYear = releaseYear, rating = rating)
