package com.farukg.movievault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farukg.movievault.data.local.entity.FavoriteEntity
import com.farukg.movievault.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT movieId FROM favorites") fun observeFavoriteIds(): Flow<List<Long>>

    @Query(
        """
        SELECT m.* FROM movies m
        INNER JOIN favorites f ON m.id = f.movieId
        ORDER BY f.createdAtEpochMillis DESC
        """
    )
    fun observeFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE movieId = :movieId)")
    suspend fun isFavorited(movieId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE movieId = :movieId") suspend fun delete(movieId: Long)
}
