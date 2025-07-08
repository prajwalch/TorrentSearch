package com.prajwalch.torrentsearch.ui

import android.content.ClipData
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.screens.SearchScreen
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel

import kotlinx.coroutines.launch

@Composable
fun TorrentSearchApp(
    searchViewModel: SearchViewModel,
    onDownloadRequest: (MagnetUri) -> Boolean,
    onMagnetLinkShareRequest: (MagnetUri) -> Unit,
    onOpenDescriptionPageRequest: (String) -> Unit,
    onShareDescriptionPageUrlRequest: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        var isTorrentClientMissing by remember { mutableStateOf(false) }
        var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

        val coroutineScope = rememberCoroutineScope()
        val clipboard = LocalClipboard.current

        if (isTorrentClientMissing) {
            TorrentClientNotFoundDialog(
                onConfirmation = { isTorrentClientMissing = false },
            )
        }

        selectedTorrent?.let { torrent ->
            val magnetLinkCopiedHint = stringResource(R.string.hint_magnet_link_copied)
            val descriptionPageUrlCopiedHint = stringResource(
                R.string.hint_description_page_url_copied
            )

            TorrentActionsBottomSheet(
                title = torrent.name,
                onDismissRequest = { selectedTorrent = null },
                onDownloadClick = {
                    isTorrentClientMissing = !onDownloadRequest(torrent.magnetUri())
                },
                onCopyMagnetLinkClick = {
                    coroutineScope.launch {
                        clipboard.copyText(text = torrent.magnetUri())
                        snackbarHostState.showSnackbar(magnetLinkCopiedHint)
                    }
                },
                onShareMagnetLinkClick = { onMagnetLinkShareRequest(torrent.magnetUri()) },
                onOpenDescriptionPageClick = {
                    onOpenDescriptionPageRequest(torrent.descriptionPageUrl)
                },
                onCopyDescriptionPageUrlClick = {
                    coroutineScope.launch {
                        clipboard.copyText(text = torrent.descriptionPageUrl)
                        snackbarHostState.showSnackbar(descriptionPageUrlCopiedHint)
                    }
                },
                onShareDescriptionPageUrlClick = {
                    onShareDescriptionPageUrlRequest(torrent.descriptionPageUrl)
                },
            )
        }

        SearchScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            viewModel = searchViewModel,
            onTorrentSelect = { selectedTorrent = it }
        )
    }
}

/** Copies the text into the clipboard. */
private suspend fun Clipboard.copyText(text: String) {
    val clipData = ClipData.newPlainText(
        /* label = */
        null,
        /* text = */
        text,
    )
    val clipEntry = ClipEntry(clipData = clipData)

    this@copyText.setClipEntry(clipEntry = clipEntry)
}