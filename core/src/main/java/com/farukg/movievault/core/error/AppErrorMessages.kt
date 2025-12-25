package com.farukg.movievault.core.error

fun AppError.userMessage(): String =
    when (this) {
        is AppError.Offline -> "You’re offline. Check your connection and try again."
        is AppError.Network -> "Network error. Please try again."
        is AppError.Http -> "Server error (HTTP $code). Please try again."
        is AppError.Serialization -> "We couldn’t read the response. Please try again."
        is AppError.Unknown -> "Something went wrong. Please try again."
    }
