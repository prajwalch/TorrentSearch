package com.prajwalch.torrentsearch

import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri

import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.ui.screens.SearchScreen
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.viewmodel.SearchScreenViewModel

class MainActivity : ComponentActivity() {
    val searchScreenViewModel: SearchScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(searchScreenViewModel, onTorrentSelect = { downloadTorrent(it) })
        }
    }

    private fun downloadTorrent(torrent: Torrent) {
        val torrentClientOpenIntent = Intent(Intent.ACTION_VIEW, torrent.magnetURL().toUri())
        startActivity(torrentClientOpenIntent)
    }
}

@Composable
fun App(searchScreenViewModel: SearchScreenViewModel, onTorrentSelect: (Torrent) -> Unit) {
    TorrentSearchTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                SearchScreen(searchScreenViewModel, onTorrentSelect)
            }
        }
    }
}