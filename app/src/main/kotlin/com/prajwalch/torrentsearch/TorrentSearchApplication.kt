package com.prajwalch.torrentsearch

import android.app.Application
import android.os.StrictMode

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade

import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.ui.crash.CrashActivity
import com.prajwalch.torrentsearch.util.TorrentSearchExceptionHandler

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TorrentSearchApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        Thread.setDefaultUncaughtExceptionHandler(
            TorrentSearchExceptionHandler(
                context = this,
                activityToLaunch = CrashActivity::class.java,
            ),
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        HttpClient.close()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .allowHardware(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}