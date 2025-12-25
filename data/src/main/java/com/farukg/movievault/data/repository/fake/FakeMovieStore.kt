package com.farukg.movievault.data.repository.fake

import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMovieStore {
    private val _details = MutableStateFlow(seedDetails())
    val details: StateFlow<List<MovieDetail>> = _details.asStateFlow()

    fun catalogFlow(): Flow<List<Movie>> = details.map { list -> list.map { it.toMovie() } }

    fun favoritesFlow(): Flow<List<Movie>> =
        details.map { list -> list.filter { it.isFavorite }.map { it.toMovie() } }

    fun movieDetailFlow(movieId: Long): Flow<MovieDetail?> =
        details.map { list -> list.firstOrNull { it.id == movieId } }

    fun toggleFavorite(movieId: Long): Boolean {
        _details.update { current ->
            current.map { d -> if (d.id == movieId) d.copy(isFavorite = !d.isFavorite) else d }
        }
        return _details.value.firstOrNull { it.id == movieId }?.isFavorite ?: false
    }

    private fun seedDetails(): List<MovieDetail> =
        listOf(
            MovieDetail(
                id = 123L,
                title = "Dune: Part Two",
                releaseYear = 2024,
                genres = listOf("Sci-Fi", "Adventure"),
                rating = 8.2,
                overview = "Paul Atreides unites with the Fremen to fight for Arrakis.",
                runtimeMinutes = 166,
                posterUrl = null,
                isFavorite = false,
            ),
            MovieDetail(
                id = 456L,
                title = "Oppenheimer",
                releaseYear = 2023,
                genres = listOf("Drama", "History"),
                rating = 8.4,
                overview = "The story of J. Robert Oppenheimer and the atomic bomb.",
                runtimeMinutes = 180,
                posterUrl = null,
                isFavorite = true,
            ),
            MovieDetail(
                id = 789L,
                title = "Spider-Man: Across the Spider-Verse",
                releaseYear = 2023,
                genres = listOf("Animation", "Action"),
                rating = 8.6,
                overview = "Miles discovers the Spider-Society across the multiverse.",
                runtimeMinutes = 140,
                posterUrl = null,
                isFavorite = false,
            ),
        )

    private fun MovieDetail.toMovie(): Movie =
        Movie(
            id = id,
            title = title,
            releaseYear = releaseYear,
            posterUrl = posterUrl,
            rating = rating,
            isFavorite = isFavorite,
        )
}
