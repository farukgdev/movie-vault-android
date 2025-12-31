package com.farukg.movievault.feature.catalog.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PosterFallbackTest {

    @Test
    fun `switches from primary to fallback on error`() {
        val primary = "https://image.tmdb.org/t/p/w342/a.jpg"
        val fallback = "https://image.tmdb.org/t/p/w500/a.jpg"

        val next = nextPosterUrlOnError(primary, primary, fallback)
        assertEquals(fallback, next)
    }

    @Test
    fun `does not switch when fallback is null`() {
        val primary = "p"
        val next = nextPosterUrlOnError(primary, primary, null)
        assertEquals(primary, next)
    }

    @Test
    fun `does not switch when already on fallback`() {
        val primary = "p"
        val fallback = "f"
        val next = nextPosterUrlOnError(fallback, primary, fallback)
        assertEquals(fallback, next)
    }
}
