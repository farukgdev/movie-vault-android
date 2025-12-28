package com.farukg.movievault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farukg.movievault.data.local.entity.CatalogRemoteKeyEntity

@Dao
interface CatalogRemoteKeysDao {

    @Query("SELECT * FROM catalog_remote_keys WHERE movieId = :movieId LIMIT 1")
    suspend fun remoteKeyByMovieId(movieId: Long): CatalogRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<CatalogRemoteKeyEntity>)

    @Query("DELETE FROM catalog_remote_keys") suspend fun clearRemoteKeys()
}
