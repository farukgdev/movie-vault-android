package com.farukg.movievault.data.di

import android.content.Context
import androidx.room.Room
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MovieVaultDatabase =
        Room.databaseBuilder(context, MovieVaultDatabase::class.java, "movievault.db").build()

    @Provides fun provideMovieDao(db: MovieVaultDatabase): MovieDao = db.movieDao()

    @Provides fun provideFavoriteDao(db: MovieVaultDatabase): FavoriteDao = db.favoriteDao()
}
