package com.prajwalch.torrentsearch

import android.app.Application
import com.prajwalch.torrentsearch.network.HttpClient
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TorrentSearch : Application() {
    override fun onTerminate() {
        super.onTerminate()
        HttpClient.close()
    }
}