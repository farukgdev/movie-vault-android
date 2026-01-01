package com.farukg.movievault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farukg.movievault.data.local.entity.FavoriteEntity
import com.farukg.movievault.data.local.model.FavoriteMovieRow
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT movieId FROM favorites") fun observeFavoriteIds(): Flow<List<Long>>

    @Query(
        """
        SELECT 
            m.id,
            m.title,
            m.releaseYear,
            m.posterUrl,
            m.rating,
            f.createdAtEpochMillis AS createdAtEpochMillis
        FROM favorites f
        INNER JOIN movies m ON m.id = f.movieId
        ORDER BY f.createdAtEpochMillis DESC
        """
    )
    fun observeFavoriteRows(): Flow<List<FavoriteMovieRow>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE movieId = :movieId)")
    suspend fun isFavorited(movieId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE movieId = :movieId") suspend fun delete(movieId: Long)
}
