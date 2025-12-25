package com.farukg.movievault.di

import com.farukg.movievault.data.repository.CatalogRepository
import com.farukg.movievault.data.repository.FavoritesRepository
import com.farukg.movievault.data.repository.fake.FakeCatalogRepository
import com.farukg.movievault.data.repository.fake.FakeFavoritesRepository
import com.farukg.movievault.data.repository.fake.FakeMovieStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton fun provideMovieStore(): FakeMovieStore = FakeMovieStore()

    @Provides
    @Singleton
    fun provideCatalogRepository(store: FakeMovieStore): CatalogRepository =
        FakeCatalogRepository(store)

    @Provides
    @Singleton
    fun provideFavoritesRepository(store: FakeMovieStore): FavoritesRepository =
        FakeFavoritesRepository(store)
}
