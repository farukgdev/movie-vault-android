package com.farukg.movievault.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.farukg.movievault.feature.catalog.navigation.CatalogRoute
import com.farukg.movievault.feature.catalog.navigation.DetailRoute
import com.farukg.movievault.feature.catalog.ui.CatalogScreen
import com.farukg.movievault.feature.catalog.ui.DetailScreen
import com.farukg.movievault.feature.favorites.navigation.FavoritesRoute
import com.farukg.movievault.feature.favorites.ui.FavoritesScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val screenModifier = Modifier.fillMaxSize()

    NavHost(navController = navController, startDestination = CatalogRoute, modifier = modifier) {
        composable<CatalogRoute> {
            CatalogScreen(
                onOpenDetail = { id -> navController.navigate(DetailRoute(id)) },
                onOpenFavorites = { navController.navigate(FavoritesRoute) },
                modifier = screenModifier,
            )
        }

        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()
            DetailScreen(
                movieId = args.movieId,
                onBack = { navController.popBackStack() },
                modifier = screenModifier,
            )
        }

        composable<FavoritesRoute> {
            FavoritesScreen(onBack = { navController.popBackStack() }, modifier = screenModifier)
        }
    }
}
