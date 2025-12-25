package com.farukg.movievault.core.error

sealed interface AppError {
    data class Network(val cause: Throwable? = null) : AppError

    data class Http(val code: Int, val cause: Throwable? = null) : AppError

    data class Serialization(val cause: Throwable? = null) : AppError

    data class Offline(val cause: Throwable? = null) : AppError

    data class Unknown(val cause: Throwable? = null) : AppError
}
