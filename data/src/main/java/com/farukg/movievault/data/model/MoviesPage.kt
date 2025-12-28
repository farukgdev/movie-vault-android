package com.farukg.movievault.data.model

data class MoviesPage(val page: Int, val totalPages: Int, val results: List<Movie>)
