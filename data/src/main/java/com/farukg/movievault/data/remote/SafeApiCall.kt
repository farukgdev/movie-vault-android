package com.farukg.movievault.data.remote

import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.result.AppResult
import java.io.IOException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

internal suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        AppResult.Error(t.toAppError())
    }

private fun Throwable.toAppError(): AppError =
    when (this) {
        is UnknownHostException -> AppError.Offline(this)
        is IOException -> AppError.Network(this)
        is HttpException -> AppError.Http(code = code(), cause = this)
        is SerializationException -> AppError.Serialization(this)
        else -> AppError.Unknown(this)
    }
