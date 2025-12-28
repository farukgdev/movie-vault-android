package com.farukg.movievault.data.local.model

import androidx.room.Embedded
import com.farukg.movievault.data.local.entity.MovieEntity

data class MovieWithFavorite(@Embedded val movie: MovieEntity, val isFavorite: Boolean)
