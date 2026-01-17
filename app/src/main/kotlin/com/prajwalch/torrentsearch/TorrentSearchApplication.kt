package com.prajwalch.torrentsearch

import android.app.Application

import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.ui.crash.CrashActivity
import com.prajwalch.torrentsearch.ui.crash.UncaughtExceptionHandler

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TorrentSearchApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        UncaughtExceptionHandler.init(
            context = this,
            crashActivity = CrashActivity::class.java,
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        HttpClient.close()
    }
}