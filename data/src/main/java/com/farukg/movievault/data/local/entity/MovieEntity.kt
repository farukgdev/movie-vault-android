package com.farukg.movievault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val releaseYear: Int?,
    val posterUrl: String?,
    val rating: Double?,
    val overview: String?,
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val popularRank: Int, // ordering for catalog
)
