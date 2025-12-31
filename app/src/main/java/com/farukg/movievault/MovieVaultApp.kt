package com.farukg.movievault

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

private const val POSTER_DISK_CACHE_BYTES: Long = 100L * 1024L * 1024L // 100 MB

@HiltAndroidApp
class MovieVaultApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(180)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.noBackupFilesDir.resolve("poster_cache"))
                    .maxSizeBytes(POSTER_DISK_CACHE_BYTES)
                    .build()
            }
            .build()
    }
}
