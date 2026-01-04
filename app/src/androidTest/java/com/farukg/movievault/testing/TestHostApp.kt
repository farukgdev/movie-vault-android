package com.farukg.movievault.testing

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.farukg.movievault.core.ui.scaffold.AppScaffoldController
import com.farukg.movievault.core.ui.scaffold.LocalAppScaffold
import com.farukg.movievault.core.ui.scaffold.ScreenKey
import com.farukg.movievault.feature.catalog.navigation.CatalogRoute
import com.farukg.movievault.feature.favorites.ui.FavoritesViewModel
import com.farukg.movievault.navigation.AppNavHost
import com.farukg.movievault.navigation.MovieVaultNavigator

@Composable
fun TestMovieVaultApp(
    backStack: SnapshotStateList<NavKey> = remember { mutableStateListOf(CatalogRoute) }
) {
    val scaffold = remember { AppScaffoldController() }
    val navigator = remember { MovieVaultNavigator(backStack) }
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()

    val active = backStack.lastOrNull() as? ScreenKey
    LaunchedEffect(active) { scaffold.updateActiveScreen(active) }

    CompositionLocalProvider(LocalAppScaffold provides scaffold) {
        MaterialTheme {
            Scaffold(topBar = { scaffold.RenderTopBar() }) { padding ->
                AppNavHost(
                    modifier = Modifier.padding(padding),
                    favoritesViewModel = favoritesViewModel,
                    backStack = backStack,
                    navigator = navigator,
                    onFinish = {},
                )
            }
        }
    }
}
