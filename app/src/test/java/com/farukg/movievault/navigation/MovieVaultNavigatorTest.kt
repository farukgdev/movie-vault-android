package com.farukg.movievault.navigation

import androidx.navigation3.runtime.NavKey
import com.farukg.movievault.feature.catalog.navigation.CatalogRoute
import com.farukg.movievault.feature.catalog.navigation.DetailRoute
import com.farukg.movievault.feature.favorites.navigation.FavoritesRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieVaultNavigatorTest {

    @Test
    fun `second navigation is ignored while push is in-flight`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute)
        val navigator = MovieVaultNavigator(backStack)

        navigator.openDetail(1L)
        navigator.openDetail(2L) // should be ignored until resumed

        assertEquals(listOf(CatalogRoute, DetailRoute(1L)), backStack)
    }

    @Test
    fun `after resumed, next navigation is allowed`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute)
        val navigator = MovieVaultNavigator(backStack)

        navigator.openDetail(1L)
        navigator.onEntryResumed(DetailRoute(1L))

        navigator.openDetail(2L)

        assertEquals(listOf(CatalogRoute, DetailRoute(1L), DetailRoute(2L)), backStack)
    }

    @Test
    fun `back during in-flight push cancels destination`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute)
        val navigator = MovieVaultNavigator(backStack)

        navigator.openDetail(1L)

        val result = navigator.onBackPressed()

        assertTrue(result is MovieVaultNavigator.BackResult.Popped)
        assertEquals(listOf(CatalogRoute), backStack)
    }

    @Test
    fun `back spam during in-flight pop returns Busy and does not pop again`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute, DetailRoute(1L), FavoritesRoute)
        val navigator = MovieVaultNavigator(backStack)

        // First back should pop Favorites
        val first = navigator.onBackPressed()
        assertTrue(first is MovieVaultNavigator.BackResult.Popped)
        assertEquals(listOf(CatalogRoute, DetailRoute(1L)), backStack)

        // Second back should be ignored as Busy
        val second = navigator.onBackPressed()
        assertTrue(second is MovieVaultNavigator.BackResult.Busy)
        assertEquals(listOf(CatalogRoute, DetailRoute(1L)), backStack)
    }

    @Test
    fun `after pop resumed, back works again`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute, DetailRoute(1L), FavoritesRoute)
        val navigator = MovieVaultNavigator(backStack)

        navigator.onBackPressed() // inFlight Pop(DetailRoute(1L))
        navigator.onEntryResumed(DetailRoute(1L)) // pop completes

        val result = navigator.onBackPressed()
        assertTrue(result is MovieVaultNavigator.BackResult.Popped)
        assertEquals(listOf(CatalogRoute), backStack)
    }

    @Test
    fun `back at root returns AtRoot`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute)
        val navigator = MovieVaultNavigator(backStack)

        val result = navigator.onBackPressed()

        assertTrue(result is MovieVaultNavigator.BackResult.AtRoot)
        assertEquals(listOf(CatalogRoute), backStack)
    }

    @Test
    fun `openFavorites ignored while in-flight`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute)
        val navigator = MovieVaultNavigator(backStack)

        navigator.openDetail(1L)
        navigator.openFavorites() // ignored

        assertEquals(listOf(CatalogRoute, DetailRoute(1L)), backStack)
    }

    @Test
    fun `openFavorites works after resumed`() {
        val backStack = mutableListOf<NavKey>(CatalogRoute)
        val navigator = MovieVaultNavigator(backStack)

        navigator.openDetail(1L)
        navigator.onEntryResumed(DetailRoute(1L))

        navigator.openFavorites()

        assertEquals(listOf(CatalogRoute, DetailRoute(1L), FavoritesRoute), backStack)
    }
}
