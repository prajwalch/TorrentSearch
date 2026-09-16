package com.prajwalch.torrentsearch.ui.torrentactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.extension.openMagnetLink
import com.prajwalch.torrentsearch.ui.extension.startTextShareIntent
import com.prajwalch.torrentsearch.ui.torrentactions.component.TorrentActionsContent
import com.prajwalch.torrentsearch.ui.torrentactions.component.TorrentFileDownloadContent

import kotlinx.coroutines.launch

import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentActionsBottomSheet(
    onDismiss: () -> Unit,
    torrent: Torrent,
    onTorrentClientNotFound: () -> Unit,
    onNavigateToDetails: (id: String, pageUrl: String, providerName: String) -> Unit,
    onShowSnackBar: (message: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentActionsViewModel = koinViewModel(
        viewModelStoreOwner = rememberViewModelStoreOwner(),
        parameters = { parametersOf(torrent) },
    ),
) {
    val magnetUriState by viewModel.magnetUriState.collectAsStateWithLifecycle()
    val torrentFileState by viewModel.torrentFileState.collectAsStateWithLifecycle()
    val isTorrentBookmarked by viewModel.isTorrentBookmarked.collectAsStateWithLifecycle()
    val openTorrentDetailsInApp by viewModel.openTorrentDetailsInApp.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val magnetLinkCopiedMessage = stringResource(R.string.torrent_message_magnet_link_copied)
    val urlCopiedMessage = stringResource(R.string.torrent_message_url_copied)

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current

    fun closeSheet() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            onDismiss()
        }
    }

    fun withDetailsPageUrl(action: (String) -> Unit): () -> Unit = {
        torrent.descriptionPageUrl?.let(action)
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        AnimatedContent(
            targetState = torrentFileState,
            contentKey = { it?.let { TorrentFileState::class } },
        ) { targetTorrentFileState ->
            if (targetTorrentFileState == null) {
                TorrentActionsContent(
                    modifier = Modifier.animateEnterExit(
                        enter = slideInHorizontally { fullWidth -> -fullWidth },
                        exit = slideOutHorizontally { fullWidth -> -fullWidth }
                    ),
                    torrent = torrent,
                    magnetUriState = magnetUriState,
                    isTorrentBookmarked = isTorrentBookmarked,
                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                    onOpenMagnetLink = { magnetUri ->
                        val torrentClientFound = context.openMagnetLink(magnetUri)
                        if (!torrentClientFound) onTorrentClientNotFound()
                        closeSheet()
                    },
                    onDownloadTorrentFile = { viewModel.downloadTorrentFile(it) },
                    onCopyMagnetLink = { magnetUri ->
                        coroutineScope.launch {
                            clipboard.copyText(magnetUri)
                            onShowSnackBar(magnetLinkCopiedMessage)
                            closeSheet()
                        }
                    },
                    onShareMagnetLink = { magnetUri ->
                        context.startTextShareIntent(magnetUri)
                        closeSheet()
                    },
                    onOpenTorrentDetails = withDetailsPageUrl {
                        if (openTorrentDetailsInApp) {
                            onNavigateToDetails(torrent.id, it, torrent.providerName)
                        } else {
                            uriHandler.openUri(it)
                        }

                        closeSheet()
                    },
                    onCopyDetailsPageLink = withDetailsPageUrl {
                        coroutineScope.launch {
                            clipboard.copyText(it)
                            onShowSnackBar(urlCopiedMessage)
                            closeSheet()
                        }
                    },
                    onShareDetailsPageLink = withDetailsPageUrl {
                        context.startTextShareIntent(it)
                        closeSheet()
                    },
                )
            } else {
                TorrentFileDownloadContent(
                    modifier = Modifier.animateEnterExit(
                        enter = slideInHorizontally { fullWidth -> fullWidth },
                        exit = slideOutHorizontally { fullWidth -> fullWidth },
                    ),
                    state = targetTorrentFileState,
                    onWriteFileContent = { viewModel.writeTorrentFileContent(it) },
                    onCloseSheet = { closeSheet() },
                    onGoBack = { viewModel.resetTorrentFileState() },
                )
            }
        }
    }
}