package com.farukg.movievault.data.remote.tmdb.mapper

import com.farukg.movievault.data.remote.tmdb.dto.GenreDto
import com.farukg.movievault.data.remote.tmdb.dto.MovieDetailDto
import com.farukg.movievault.data.remote.tmdb.dto.MovieDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbDtoMappersTest {

    private val imageBaseUrlNoSlash = "https://image.tmdb.org/t/p/w500"
    private val imageBaseUrlWithSlash = "https://image.tmdb.org/t/p/w500/"

    @Test
    fun `MovieDto maps release year and poster url`() {
        val dto =
            MovieDto(
                id = 1L,
                title = "A",
                releaseDate = "2020-01-01",
                posterPath = "/abc.jpg",
                voteAverage = 7.5,
            )

        val movie = dto.toDomain(imageBaseUrlNoSlash)

        assertEquals(1L, movie.id)
        assertEquals("A", movie.title)
        assertEquals(2020, movie.releaseYear)
        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", movie.posterUrl)
        assertEquals(7.5, movie.rating)
    }

    @Test
    fun `MovieDto maps invalid release date to null year`() {
        val dto =
            MovieDto(
                id = 1L,
                title = "A",
                releaseDate = "not-a-date",
                posterPath = null,
                voteAverage = null,
            )

        val movie = dto.toDomain(imageBaseUrlNoSlash)

        assertNull(movie.releaseYear)
        assertNull(movie.posterUrl)
        assertNull(movie.rating)
    }

    @Test
    fun `MovieDto trims trailing slash in image base url`() {
        val dto =
            MovieDto(
                id = 1L,
                title = "A",
                releaseDate = "2020-01-01",
                posterPath = "/abc.jpg",
                voteAverage = null,
            )

        val movie = dto.toDomain(imageBaseUrlWithSlash)

        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", movie.posterUrl)
    }

    @Test
    fun `MovieDetailDto maps genres overview and poster`() {
        val dto =
            MovieDetailDto(
                id = 9L,
                title = "Detail",
                releaseDate = "2019-12-31",
                genres = listOf(GenreDto(1, "Action"), GenreDto(2, "Drama")),
                voteAverage = 8.2,
                overview = null,
                runtime = 123,
                posterPath = "/p.jpg",
            )

        val detail = dto.toDomain(imageBaseUrlNoSlash)

        assertEquals(9L, detail.id)
        assertEquals("Detail", detail.title)
        assertEquals(2019, detail.releaseYear)
        assertEquals(listOf("Action", "Drama"), detail.genres)
        assertEquals(8.2, detail.rating)
        assertEquals("", detail.overview) // null -> empty string
        assertEquals(123, detail.runtimeMinutes)
        assertEquals("https://image.tmdb.org/t/p/w500/p.jpg", detail.posterUrl)
    }
}
