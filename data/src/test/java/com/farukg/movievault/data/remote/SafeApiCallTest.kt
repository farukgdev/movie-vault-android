package com.farukg.movievault.data.remote

import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SafeApiCallTest {

    @Test
    fun `UnknownHostException maps to Offline`() = runTest {
        val result = safeApiCall<Unit> { throw UnknownHostException("no host") }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.Offline)
    }

    @Test
    fun `IOException maps to Network`() = runTest {
        val result = safeApiCall<Unit> { throw IOException("io") }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.Network)
    }

    @Test
    fun `HttpException maps to Http with code`() = runTest {
        val responseBody = "nope".toResponseBody("application/json".toMediaType())
        val httpEx = HttpException(Response.error<Unit>(404, responseBody))

        val result = safeApiCall<Unit> { throw httpEx }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.Http)
        assertEquals(404, (error as AppError.Http).code)
    }

    @Test
    fun `SerializationException maps to Serialization`() = runTest {
        val result = safeApiCall<Unit> { throw SerializationException("bad json") }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.Serialization)
    }

    @Test
    fun `Unknown exception maps to Unknown`() = runTest {
        val result = safeApiCall<Unit> { throw IllegalStateException("boom") }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.Unknown)
    }

    @Test
    fun `CancellationException is rethrown`() {
        assertThrows(CancellationException::class.java) {
            runTest { safeApiCall<Unit> { throw CancellationException("cancel") } }
        }
    }
}
