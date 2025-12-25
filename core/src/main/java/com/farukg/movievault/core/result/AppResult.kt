package com.farukg.movievault.core.result

import com.farukg.movievault.core.error.AppError

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>

    data class Error(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Error -> this
    }

inline fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
