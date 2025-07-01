package com.prajwalch.torrentsearch

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri

import com.prajwalch.torrentsearch.data.MagnetUri
import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.ui.TorrentSearchApp
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModelFactory

class MainActivity : ComponentActivity() {
    private val searchViewModel: SearchViewModel by viewModels {
        val torrentsRepository = TorrentsRepository(httpClient = HttpClient)
        SearchViewModelFactory(torrentsRepository = torrentsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TorrentSearchTheme {
                TorrentSearchApp(
                    searchViewModel = searchViewModel,
                    onDownloadRequest = ::downloadTorrentViaClient
                )
            }
        }
    }

    /**
     * Starts the available torrent client for downloading the given torrent.
     *
     * @return `true` if the client starts successfully, `false` otherwise.
     */
    private fun downloadTorrentViaClient(magnetUri: MagnetUri): Boolean {
        val torrentClientOpenIntent = Intent(Intent.ACTION_VIEW, magnetUri.toUri())

        return try {
            startActivity(torrentClientOpenIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}