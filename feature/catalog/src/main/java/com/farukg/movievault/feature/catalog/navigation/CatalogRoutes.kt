package com.farukg.movievault.feature.catalog.navigation

import androidx.navigation3.runtime.NavKey
import com.farukg.movievault.core.ui.scaffold.ScreenKey
import kotlinx.serialization.Serializable

@Serializable data object CatalogRoute : NavKey, ScreenKey

@Serializable
data class DetailRoute(val movieId: Long, val initialTitle: String? = null) : NavKey, ScreenKey
