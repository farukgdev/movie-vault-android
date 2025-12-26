package com.farukg.movievault.feature.catalog.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farukg.movievault.core.result.AppResult
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CatalogViewModel @Inject constructor(repository: CatalogRepository) : ViewModel() {
    init {
        viewModelScope.launch { repository.refreshCatalog(force = false) }
    }

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

    fun retry() {
        // For now no-op
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
