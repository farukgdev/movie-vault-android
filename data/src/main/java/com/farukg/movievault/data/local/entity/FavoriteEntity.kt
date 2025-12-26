package com.farukg.movievault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val movieId: Long, val createdAtEpochMillis: Long)
