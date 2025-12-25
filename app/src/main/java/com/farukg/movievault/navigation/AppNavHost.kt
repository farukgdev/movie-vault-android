package com.farukg.movievault.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.farukg.movievault.feature.catalog.navigation.CatalogRoute
import com.farukg.movievault.feature.catalog.navigation.DetailRoute
import com.farukg.movievault.feature.catalog.ui.catalog.CatalogRouteScreen
import com.farukg.movievault.feature.catalog.ui.detail.DetailRouteScreen
import com.farukg.movievault.feature.favorites.navigation.FavoritesRoute
import com.farukg.movievault.feature.favorites.ui.FavoritesRouteScreen

fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun ReportResumed(key: NavKey, navigator: MovieVaultNavigator) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, key) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                navigator.onEntryResumed(key)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val activity = LocalContext.current.findActivity()

    val backStack = rememberNavBackStack(CatalogRoute)
    val navigator = remember(backStack) { MovieVaultNavigator(backStack) }

    val screenModifier = Modifier.fillMaxSize()

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = {
            when (navigator.onBackPressed()) {
                MovieVaultNavigator.BackResult.Popped -> Unit
                MovieVaultNavigator.BackResult.Busy -> Unit
                MovieVaultNavigator.BackResult.AtRoot -> activity?.finish()
            }
        },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        transitionSpec = {
            fadeIn(
                animationSpec =
                    tween(durationMillis = 210, delayMillis = 35, easing = LinearOutSlowInEasing)
            ) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing))
        },
        popTransitionSpec = {
            fadeIn(
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
            ) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing))
        },
        predictivePopTransitionSpec = {
            fadeIn(
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
                initialAlpha = 0f,
            ) togetherWith
                fadeOut(
                    animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing),
                    targetAlpha = 0f,
                )
        },
        entryProvider =
            entryProvider {
                entry<CatalogRoute> {
                    ReportResumed(CatalogRoute, navigator)
                    CatalogRouteScreen(
                        onOpenDetail = { id -> navigator.openDetail(id) },
                        onOpenFavorites = { navigator.openFavorites() },
                        modifier = screenModifier,
                    )
                }

                entry<DetailRoute> { key ->
                    ReportResumed(key, navigator)
                    DetailRouteScreen(
                        movieId = key.movieId,
                        onBack = { navigator.onBackPressed() },
                        modifier = screenModifier,
                    )
                }

                entry<FavoritesRoute> {
                    ReportResumed(FavoritesRoute, navigator)
                    FavoritesRouteScreen(
                        onBack = { navigator.onBackPressed() },
                        onOpenDetail = { id -> navigator.openDetail(id) },
                        modifier = screenModifier,
                    )
                }
            },
    )
}
