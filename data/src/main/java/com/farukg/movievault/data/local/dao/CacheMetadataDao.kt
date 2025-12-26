package com.farukg.movievault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farukg.movievault.data.local.entity.CacheMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheMetadataDao {

    @Query("SELECT * FROM cache_metadata WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CacheMetadataEntity?

    @Query("SELECT lastUpdatedEpochMillis FROM cache_metadata WHERE `key` = :key LIMIT 1")
    fun observeLastUpdated(key: String): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: CacheMetadataEntity)
}
