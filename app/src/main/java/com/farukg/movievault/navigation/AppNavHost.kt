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
import com.farukg.movievault.feature.catalog.ui.CatalogRouteScreen
import com.farukg.movievault.feature.catalog.ui.DetailRouteScreen
import com.farukg.movievault.feature.favorites.navigation.FavoritesRoute
import com.farukg.movievault.feature.favorites.ui.FavoritesRouteScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val screenModifier = Modifier.fillMaxSize()

    NavHost(navController = navController, startDestination = CatalogRoute, modifier = modifier) {
        composable<CatalogRoute> {
            CatalogRouteScreen(
                onOpenDetail = { id ->
                    navController.navigate(DetailRoute(id)) { launchSingleTop = true }
                },
                onOpenFavorites = {
                    navController.navigate(FavoritesRoute) { launchSingleTop = true }
                },
                modifier = screenModifier,
            )
        }

        composable<DetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()
            DetailRouteScreen(
                movieId = args.movieId,
                onBack = { navController.popBackStack() },
                modifier = screenModifier,
            )
        }

        composable<FavoritesRoute> {
            FavoritesRouteScreen(
                onBack = { navController.popBackStack() },
                modifier = screenModifier,
            )
        }
    }
}
