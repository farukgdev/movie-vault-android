package com.farukg.movievault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farukg.movievault.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies ORDER BY popularRank ASC")
    fun observeCatalog(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId LIMIT 1")
    fun observeMovie(movieId: Long): Flow<MovieEntity?>

    @Query("SELECT * FROM movies WHERE id = :movieId LIMIT 1")
    suspend fun getMovie(movieId: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    suspend fun getMoviesByIds(ids: List<Long>): List<MovieEntity>

    @Query("SELECT COUNT(*) FROM movies") suspend fun countMovies(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: MovieEntity)
}
