package com.farukg.movievault.testing

import android.content.Context
import androidx.room.Room
import com.farukg.movievault.core.time.Clock
import com.farukg.movievault.data.di.DatabaseModule
import com.farukg.movievault.data.di.NetworkModule
import com.farukg.movievault.data.local.dao.CacheMetadataDao
import com.farukg.movievault.data.local.dao.CatalogRemoteKeysDao
import com.farukg.movievault.data.local.dao.FavoriteDao
import com.farukg.movievault.data.local.dao.MovieDao
import com.farukg.movievault.data.local.db.MovieVaultDatabase
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.di.AppModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

class TestClock(startMs: Long) : Clock {
    var nowMs: Long = startMs
        private set

    override fun now(): Long = nowMs
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object FakeNetworkModule {

    @Provides
    @Singleton
    fun provideFakeRemote(): FakeCatalogRemoteDataSource = FakeCatalogRemoteDataSource()

    @Provides
    @Singleton
    fun provideCatalogRemoteDataSource(fake: FakeCatalogRemoteDataSource): CatalogRemoteDataSource =
        fake
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object InMemoryDatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): MovieVaultDatabase =
        Room.inMemoryDatabaseBuilder(context, MovieVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides fun provideMovieDao(db: MovieVaultDatabase): MovieDao = db.movieDao()

    @Provides fun provideFavoriteDao(db: MovieVaultDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideCacheMetadataDao(db: MovieVaultDatabase): CacheMetadataDao = db.cacheMetadataDao()

    @Provides
    fun provideRemoteKeysDao(db: MovieVaultDatabase): CatalogRemoteKeysDao =
        db.catalogRemoteKeysDao()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AppModule::class])
object TestTimeModule {

    @Provides @Singleton fun provideTestClock(): TestClock = TestClock(startMs = 1_700_000_000_000L)

    @Provides @Singleton fun provideClock(testClock: TestClock): Clock = testClock
}
