package com.prajwalch.torrentsearch.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.screens.SearchScreen
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel

@Composable
fun TorrentSearchApp(
    searchViewModel: SearchViewModel,
    onDownloadRequest: (MagnetUri) -> Boolean,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        var isTorrentClientMissing by remember { mutableStateOf(false) }

        if (isTorrentClientMissing) {
            TorrentClientNotFoundDialog(
                onConfirmation = { isTorrentClientMissing = false }
            )
        }

        SearchScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            viewModel = searchViewModel,
            onTorrentSelect = { magnetUri ->
                isTorrentClientMissing = !onDownloadRequest(magnetUri)
            }
        )
    }
}