package com.farukg.movievault.testing

import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import com.farukg.movievault.data.local.entity.MovieEntity
import com.farukg.movievault.data.model.Movie
import com.farukg.movievault.data.model.MovieDetail
import com.farukg.movievault.data.model.MoviesPage

data class TestMovieFixture(
    val id: Long,
    val title: String,
    val releaseYear: Int,
    val rating: Double,
    val overview: String,
    val runtimeMinutes: Int,
    val genres: List<String>,
    val posterUrl: String? = null,
) {
    val runtimeText: String
        get() = "${runtimeMinutes}m"
}

object TestMovies {
    const val DEFAULT_YEAR = 2024
    const val DEFAULT_RUNTIME_MIN = 123
    const val DEFAULT_RATING = 7.2
    val DEFAULT_GENRES: List<String> = listOf("Action", "Drama")

    fun title(id: Long): String = "Movie $id"

    fun overview(id: Long): String = "Overview for Movie $id"

    fun fixture(
        id: Long,
        year: Int = DEFAULT_YEAR,
        rating: Double = DEFAULT_RATING,
        runtimeMinutes: Int = DEFAULT_RUNTIME_MIN,
        genres: List<String> = DEFAULT_GENRES,
        posterUrl: String? = null,
    ): TestMovieFixture =
        TestMovieFixture(
            id = id,
            title = title(id),
            releaseYear = year,
            rating = rating,
            overview = overview(id),
            runtimeMinutes = runtimeMinutes,
            genres = genres,
            posterUrl = posterUrl,
        )
}

fun moviesPage(
    page: Int,
    totalPages: Int,
    ids: LongRange,
    year: Int = TestMovies.DEFAULT_YEAR,
): MoviesPage {
    val results =
        ids.map { id ->
            val f = TestMovies.fixture(id = id, year = year)
            Movie(
                id = f.id,
                title = f.title,
                releaseYear = f.releaseYear,
                posterUrl = null,
                rating = f.rating,
                isFavorite = false,
            )
        }
    return MoviesPage(page = page, totalPages = totalPages, results = results)
}

fun movieDetail(
    id: Long,
    year: Int = TestMovies.DEFAULT_YEAR,
    detailFetchedAtEpochMillis: Long? = null,
): MovieDetail {
    val f = TestMovies.fixture(id = id, year = year)
    return MovieDetail(
        id = f.id,
        title = f.title,
        releaseYear = f.releaseYear,
        genres = f.genres,
        rating = f.rating,
        overview = f.overview,
        runtimeMinutes = f.runtimeMinutes,
        posterUrl = null,
        isFavorite = false,
        detailFetchedAtEpochMillis = detailFetchedAtEpochMillis,
    )
}

fun seedCatalogEntities(ids: LongRange, year: Int = TestMovies.DEFAULT_YEAR): List<MovieEntity> =
    ids.mapIndexed { index, id ->
        val f = TestMovies.fixture(id = id, year = year)
        MovieEntity(
            id = f.id,
            title = f.title,
            releaseYear = f.releaseYear,
            posterUrl = null,
            rating = f.rating,
            overview = null,
            runtimeMinutes = null,
            genres = emptyList(),
            popularRank = index,
            detailFetchedAtEpochMillis = null,
        )
    }

fun fullMovieEntity(
    id: Long,
    rank: Int = 0,
    nowMs: Long,
    year: Int = TestMovies.DEFAULT_YEAR,
): MovieEntity {
    val f = TestMovies.fixture(id = id, year = year)
    return MovieEntity(
        id = f.id,
        title = f.title,
        releaseYear = f.releaseYear,
        posterUrl = null,
        rating = f.rating,
        overview = f.overview,
        runtimeMinutes = f.runtimeMinutes,
        genres = f.genres,
        popularRank = rank,
        detailFetchedAtEpochMillis = nowMs,
    )
}

fun cacheUpdated(key: String, atMs: Long) =
    CacheMetadataEntity(key = key, lastUpdatedEpochMillis = atMs)
