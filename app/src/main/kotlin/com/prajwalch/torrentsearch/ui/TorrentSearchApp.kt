package com.prajwalch.torrentsearch.ui

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

import com.prajwalch.torrentsearch.data.MagnetUri
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.screens.SearchScreen
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel

@Composable
fun TorrentSearchApp(
    searchViewModel: SearchViewModel,
    onDownloadRequest: (MagnetUri) -> Boolean,
) {
    var isTorrentClientMissing by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (isTorrentClientMissing) {
                TorrentClientNotFoundDialog(
                    onConfirmation = { isTorrentClientMissing = false }
                )
            }
            SearchScreen(
                viewModel = searchViewModel,
                onTorrentSelect = {
                    isTorrentClientMissing = !onDownloadRequest(it.magnetUri())
                }
            )
        }
    }
}