package com.farukg.movievault.data.di

import com.farukg.movievault.data.BuildConfig
import com.farukg.movievault.data.remote.CatalogRemoteDataSource
import com.farukg.movievault.data.remote.tmdb.TmdbApiService
import com.farukg.movievault.data.remote.tmdb.TmdbCatalogRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder =
            OkHttpClient.Builder().addInterceptor(TmdbApiKeyInterceptor(BuildConfig.TMDB_API_KEY))

        if (BuildConfig.DEBUG) {
            val logging =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideTmdbApiService(retrofit: Retrofit): TmdbApiService =
        retrofit.create(TmdbApiService::class.java)

    @Provides
    @Singleton
    fun provideCatalogRemoteDataSource(api: TmdbApiService): CatalogRemoteDataSource =
        TmdbCatalogRemoteDataSource(api = api, imageBaseUrl = BuildConfig.TMDB_IMAGE_BASE_URL)
}

private class TmdbApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (apiKey.isBlank()) return chain.proceed(req)

        val newUrl = req.url.newBuilder().addQueryParameter("api_key", apiKey).build()

        return chain.proceed(req.newBuilder().url(newUrl).build())
    }
}
