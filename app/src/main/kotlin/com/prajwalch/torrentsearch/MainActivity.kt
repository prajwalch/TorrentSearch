package com.prajwalch.torrentsearch

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri

import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.screens.SearchScreen
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.viewmodel.SearchScreenViewModel

class MainActivity : ComponentActivity() {
    val searchScreenViewModel: SearchScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(searchScreenViewModel, downloadTorrentViaClient = ::downloadTorrentViaBrowser)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchScreenViewModel.closeConnection()
    }

    /**
     * Starts the available torrent client for downloading the given torrent.
     *
     * @return `true` if the client starts successfully, `false` otherwise.
     */
    private fun downloadTorrentViaClient(torrent: Torrent): Boolean {
        val torrentClientOpenIntent = Intent(Intent.ACTION_VIEW, torrent.magnetURL().toUri())

        return try {
            startActivity(torrentClientOpenIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun downloadTorrentViaBrowser(torrent: Torrent): Boolean {
        val url = "https://webtor.io/${torrent.hash}"
        val customTabIntent = CustomTabsIntent.Builder().build()

        customTabIntent.launchUrl(this, url.toUri())

        return true
    }
}

@Composable
fun App(
    searchScreenViewModel: SearchScreenViewModel,
    downloadTorrentViaClient: (Torrent) -> Boolean
) {
    var isTorrentClientMissing by remember { mutableStateOf(false) }

    TorrentSearchTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (isTorrentClientMissing) {
                    TorrentClientNotFoundDialog(
                        onConfirmation = { isTorrentClientMissing = false }
                    )
                }
                SearchScreen(searchScreenViewModel, onTorrentSelect = {
                    isTorrentClientMissing = !downloadTorrentViaClient(it)
                })
            }
        }
    }
}