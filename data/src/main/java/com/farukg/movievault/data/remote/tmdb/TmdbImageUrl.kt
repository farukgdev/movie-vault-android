package com.farukg.movievault.data.remote.tmdb

enum class TmdbImageSize(val segment: String) {
    List("w342"),
    Detail("w500"),
}

fun String.tmdbWithSize(size: TmdbImageSize): String {
    val marker = "/t/p/"
    val idx = indexOf(marker)
    if (idx == -1) return this

    val sizeStart = idx + marker.length
    val sizeEnd = indexOf('/', startIndex = sizeStart)
    if (sizeEnd == -1) return this

    val current = substring(sizeStart, sizeEnd)
    if (current == size.segment) return this

    return replaceRange(sizeStart, sizeEnd, size.segment)
}

fun String?.tmdbWithSizeOrNull(size: TmdbImageSize): String? =
    this?.trim()?.takeUnless { it.isBlank() }?.tmdbWithSize(size)
