package com.farukg.movievault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_remote_keys")
data class CatalogRemoteKeyEntity(
    @PrimaryKey val movieId: Long,
    val prevKey: Int?,
    val nextKey: Int?,
)
