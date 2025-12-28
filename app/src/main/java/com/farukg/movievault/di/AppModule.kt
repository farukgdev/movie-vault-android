package com.farukg.movievault.di

import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.core.time.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton fun provideClock(): Clock = SystemClock
}
