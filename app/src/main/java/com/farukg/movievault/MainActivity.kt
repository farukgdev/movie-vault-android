package com.farukg.movievault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.rememberNavBackStack
import com.farukg.movievault.core.ui.scaffold.AppScaffoldController
import com.farukg.movievault.core.ui.scaffold.LocalAppScaffold
import com.farukg.movievault.core.ui.scaffold.ScreenKey
import com.farukg.movievault.feature.catalog.navigation.CatalogRoute
import com.farukg.movievault.feature.favorites.ui.FavoritesViewModel
import com.farukg.movievault.navigation.AppNavHost
import com.farukg.movievault.navigation.MovieVaultNavigator
import com.farukg.movievault.ui.theme.MovieVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieVaultTheme {
                val favoritesVm: FavoritesViewModel = hiltViewModel()

                val backStack = rememberNavBackStack(CatalogRoute)
                val navigator = remember(backStack) { MovieVaultNavigator(backStack) }

                val scaffoldController = remember { AppScaffoldController() }

                val current = backStack.lastOrNull() as? ScreenKey
                SideEffect { scaffoldController.updateActiveScreen(current) }

                CompositionLocalProvider(LocalAppScaffold provides scaffoldController) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { scaffoldController.RenderTopBar() },
                        contentWindowInsets =
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                            ),
                    ) { innerPadding ->
                        AppNavHost(
                            modifier = Modifier.padding(innerPadding),
                            favoritesViewModel = favoritesVm,
                            backStack = backStack,
                            navigator = navigator,
                            onFinish = { finish() },
                        )
                    }
                }
            }
        }
    }
}
