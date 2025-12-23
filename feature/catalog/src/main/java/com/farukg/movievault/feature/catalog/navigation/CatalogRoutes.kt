package com.farukg.movievault.feature.catalog.navigation

import kotlinx.serialization.Serializable

@Serializable object CatalogRoute

@Serializable data class DetailRoute(val movieId: Long)
