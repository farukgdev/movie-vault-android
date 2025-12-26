package com.farukg.movievault.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import com.farukg.movievault.data.local.entity.FavoriteEntity
import com.farukg.movievault.data.local.entity.MovieEntity

@Database(
    entities = [MovieEntity::class, FavoriteEntity::class, CacheMetadataEntity::class],
    version = 2,
)
@TypeConverters(RoomConverters::class)
abstract class MovieVaultDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun cacheMetadataDao(): CacheMetadataDao
}
