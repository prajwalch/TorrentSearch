package com.prajwalch.torrentsearch

import android.app.Application

import com.prajwalch.torrentsearch.data.repository.BookmarksRepository
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.ui.crash.CrashActivity
import com.prajwalch.torrentsearch.util.TorrentSearchExceptionHandler

import dagger.hilt.android.HiltAndroidApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltAndroidApp
class TorrentSearchApplication : Application() {
    @Inject
    lateinit var bookmarksRepository: BookmarksRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler(
            TorrentSearchExceptionHandler(
                context = this,
                activityToLaunch = CrashActivity::class.java,
            ),
        )

        // Fix bookmark IDs that may have been generated incorrectly during migration
        applicationScope.launch {
            bookmarksRepository.fixMigratedBookmarkIds()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        HttpClient.close()
    }
}