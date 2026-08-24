package com.prajwalch.torrentsearch

import android.app.Application
import android.os.StrictMode

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade

import com.prajwalch.torrentsearch.di.ViewModelModule
import com.prajwalch.torrentsearch.di.builtinSearchProvidersModule
import com.prajwalch.torrentsearch.di.dataStoreModule
import com.prajwalch.torrentsearch.di.databaseModule
import com.prajwalch.torrentsearch.di.domainModule
import com.prajwalch.torrentsearch.di.networkModule
import com.prajwalch.torrentsearch.di.repositoryModule
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.ui.crash.CrashActivity
import com.prajwalch.torrentsearch.util.TorrentSearchExceptionHandler

import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.plugin.module.dsl.module

class TorrentSearchApplication : Application(), SingletonImageLoader.Factory {
    private val networkClient: NetworkClient by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TorrentSearchApplication)
            modules(
                builtinSearchProvidersModule,
                dataStoreModule,
                databaseModule,
                domainModule,
                networkModule,
                repositoryModule,
            )
            module<ViewModelModule>()
        }

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

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(networkClient.coilKtorClient))
            }
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