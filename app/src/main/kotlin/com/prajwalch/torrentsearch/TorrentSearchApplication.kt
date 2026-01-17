package com.prajwalch.torrentsearch

import android.app.Application

import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.ui.crash.CrashActivity
import com.prajwalch.torrentsearch.ui.crash.TorrentSearchExceptionHandler

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TorrentSearchApplication : Application() {
    override fun onCreate() {
        super.onCreate()

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
}