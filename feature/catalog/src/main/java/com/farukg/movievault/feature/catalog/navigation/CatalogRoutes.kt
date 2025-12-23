package com.farukg.movievault.feature.catalog.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object CatalogRoute : NavKey

@Serializable data class DetailRoute(val movieId: Long) : NavKey
