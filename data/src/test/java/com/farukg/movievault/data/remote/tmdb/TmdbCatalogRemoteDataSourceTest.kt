package com.farukg.movievault.data.remote.tmdb

import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TmdbCatalogRemoteDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TmdbApiService

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val retrofit =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

        api = retrofit.create(TmdbApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `popular movies - parses json and maps to domain`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "page": 1,
                      "results": [
                        {
                          "id": 123,
                          "title": "Test Movie",
                          "release_date": "2020-01-01",
                          "poster_path": "/abc.jpg",
                          "vote_average": 7.5
                        }
                      ],
                      "total_pages": 1,
                      "total_results": 1
                    }
                    """
                        .trimIndent()
                )
        )

        val imageBaseUrl = "https://image.tmdb.org/t/p/w500"
        val ds = TmdbCatalogRemoteDataSource(api = api, imageBaseUrl = imageBaseUrl)
        val result = ds.fetchPopular(1)

        assertTrue(result is AppResult.Success)
        val movies = (result as AppResult.Success).data
        assertEquals(1, movies.size)
        assertEquals(123L, movies[0].id)
        assertEquals("Test Movie", movies[0].title)
        assertEquals(2020, movies[0].releaseYear)
        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", movies[0].posterUrl)

        // proves the correct endpoint was hit with the page query
        val req = server.takeRequest()
        assertTrue(req.path!!.startsWith("/movie/popular"))
        assertTrue(req.path!!.contains("page=1"))
    }

    @Test
    fun `popular movies - http error becomes AppError_Http`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"status_code":7,"status_message":"Invalid API key"}""")
        )

        val imageBaseUrl = "https://image.tmdb.org/t/p/w500"
        val ds = TmdbCatalogRemoteDataSource(api = api, imageBaseUrl = imageBaseUrl)
        val result = ds.fetchPopular(1)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.Http)
        assertEquals(401, (error as AppError.Http).code)
    }
}
