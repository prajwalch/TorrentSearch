package com.prajwalch.torrentsearch

import android.app.Application
import com.prajwalch.torrentsearch.network.HttpClient

class TorrentSearchApplication : Application() {
    override fun onTerminate() {
        super.onTerminate()
        HttpClient.close()
    }
}