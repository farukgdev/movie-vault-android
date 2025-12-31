package com.farukg.movievault.data.remote.tmdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbImageUrlTest {

    @Test
    fun `rewrites size segment to w342`() {
        val input = "https://image.tmdb.org/t/p/w500/abc.jpg"
        val out = input.tmdbWithSize(TmdbImageSize.List)
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", out)
    }

    @Test
    fun `rewrite is idempotent`() {
        val input = "https://image.tmdb.org/t/p/w342/abc.jpg"
        val out = input.tmdbWithSize(TmdbImageSize.List).tmdbWithSize(TmdbImageSize.List)

        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", out)
    }

    @Test
    fun `non tmdb urls are unchanged`() {
        val input = "https://example.com/image.jpg"
        val out = input.tmdbWithSize(TmdbImageSize.List)
        assertEquals(input, out)
    }

    @Test
    fun `null or blank returns null`() {
        assertNull(null.tmdbWithSizeOrNull(TmdbImageSize.List))
        assertNull("   ".tmdbWithSizeOrNull(TmdbImageSize.List))
    }
}
