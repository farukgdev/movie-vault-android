package com.farukg.movievault.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import com.farukg.movievault.feature.catalog.navigation.DetailRoute
import com.farukg.movievault.feature.favorites.navigation.FavoritesRoute

@Stable
class MovieVaultNavigator(private val backStack: MutableList<NavKey>) {
    private sealed interface InFlight {
        data class Push(val key: NavKey) : InFlight

        data class Pop(val key: NavKey?) : InFlight
    }

    private var inFlight: InFlight? = null

    fun openDetail(movieId: Long, initialTitle: String? = null) =
        navigateTo(DetailRoute(movieId, initialTitle))

    fun openFavorites() = navigateTo(FavoritesRoute)

    fun onEntryResumed(key: NavKey) {
        when (val f = inFlight) {
            is InFlight.Push -> if (f.key == key) inFlight = null
            is InFlight.Pop -> if (f.key == null || f.key == key) inFlight = null
            null -> Unit
        }
    }

    fun onBackPressed(): BackResult {
        return when (val f = inFlight) {
            // Mid-push, back cancels it
            is InFlight.Push -> {
                if (backStack.lastOrNull() == f.key) {
                    backStack.removeAt(backStack.lastIndex)
                }
                val newTop = backStack.lastOrNull()
                inFlight = InFlight.Pop(newTop)
                BackResult.Popped
            }

            // Mid-pop, ignore spam
            is InFlight.Pop -> BackResult.Busy

            // Normal back
            null -> {
                if (backStack.size <= 1) return BackResult.AtRoot
                backStack.removeAt(backStack.lastIndex)
                val newTop = backStack.lastOrNull()
                inFlight = InFlight.Pop(newTop)
                BackResult.Popped
            }
        }
    }

    private fun navigateTo(key: NavKey) {
        if (inFlight != null) return // first tap wins
        if (backStack.lastOrNull() == key) return

        inFlight = InFlight.Push(key)
        backStack.add(key)
    }

    sealed interface BackResult {
        data object Popped : BackResult

        data object AtRoot : BackResult

        data object Busy : BackResult
    }
}
