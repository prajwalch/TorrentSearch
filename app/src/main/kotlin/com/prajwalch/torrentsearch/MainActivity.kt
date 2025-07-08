package com.prajwalch.torrentsearch

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri

import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.models.MagnetUri
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
                    onDownloadRequest = ::downloadTorrentViaClient,
                    onMagnetLinkShareRequest = ::shareMagnetLink,
                    onOpenDescriptionPageRequest = ::openDescriptionPage,
                    onShareDescriptionPageUrlRequest = ::shareDescriptionPageUrl,
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
            Log.e(TAG, "Torrent client launch failed. (Activity not found)")
            false
        }
    }

    /** Starts the application chooser to share magnet uri with. */
    private fun shareMagnetLink(magnetUri: MagnetUri) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, magnetUri)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)

        try {
            startActivity(shareIntent)
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "Magnet uri share intent launch failed. (Activity not found)")
        }
    }

    /** Opens a description page in a default browser. */
    private fun openDescriptionPage(url: String) {
        val openPageIntent = Intent(Intent.ACTION_VIEW, url.toUri())

        try {
            startActivity(openPageIntent)
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "Failed to open description page. (Activity not found)")
        }
    }

    /** Starts the application chooser to share url with. */
    private fun shareDescriptionPageUrl(url: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/html"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)

        try {
            startActivity(shareIntent)
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "Description page URL share intent launch failed. (Activity not found)")
        }
    }

    private companion object {
        private const val TAG = "MainActivity"
    }
}