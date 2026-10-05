package com.prajwalch.torrentsearch.ui.tv.torrentactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvFocusDefaults
import com.prajwalch.torrentsearch.ui.tv.theme.spaces
import com.prajwalch.torrentsearch.ui.tv.theme.tvElevatedSurface
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.extension.openMagnetLink
import com.prajwalch.torrentsearch.ui.torrentactions.MagnetLinkState
import com.prajwalch.torrentsearch.ui.torrentactions.TorrentActionsViewModel
import com.prajwalch.torrentsearch.ui.torrentactions.TorrentFileState

import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The torrent action sheet, rebuilt as a TV dialog.
 *
 * `androidx.tv.material3` provides no bottom sheet, and `ModalBottomSheet` from
 * `androidx.compose.material3` has no D-pad focus containment - focus leaks to
 * the list behind it. A `Dialog` traps focus correctly on a remote, so the
 * action set is presented that way instead.
 *
 * Feature differences from the handheld sheet:
 *
 * - **Copy actions removed.** The Android TV clipboard has no user-visible
 *   affordance and is unavailable on several TV builds; a "Copied!" snackbar
 *   with no paste path is a dead end, so copy is not offered.
 * - **Share removed.** Google TV exposes no share targets, and the handheld
 *   `startTextShareIntent` swallows `ActivityNotFoundException`, which would fail
 *   silently.
 * - **Open magnet kept.** qBittorrent and Transmission both ship Android TV
 *   builds, so this is the primary way to use the app from the sofa.
 * - **Save .torrent kept** via SAF, but only when a document picker actually
 *   exists on the device.
 */
@Composable
fun TvTorrentActionsDialog(
    torrent: Torrent,
    onDismiss: () -> Unit,
    onViewDetails: (id: String, pageUrl: String, providerName: String) -> Unit,
    onTorrentClientNotFound: () -> Unit = {},
) {
    val context = LocalContext.current

    val viewModel: TorrentActionsViewModel = koinViewModel(
        // A fresh store owner per invocation: the ViewModel takes the torrent as
        // an injected param, so it must not be shared with a previous selection.
        viewModelStoreOwner = rememberViewModelStoreOwner(),
        parameters = { parametersOf(torrent) },
    )

    val magnetLinkState by viewModel.magnetLinkState.collectAsStateWithLifecycle()
    val torrentFileState by viewModel.torrentFileState.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isTorrentBookmarked.collectAsStateWithLifecycle()

    val createTorrentFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-bittorrent"),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let(viewModel::writeTorrentFileContent)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
    ) {
        androidx.tv.material3.Surface(
            shape = MaterialTheme.shapes.extraLarge,
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.tvElevatedSurface,
            ),
            modifier = Modifier.fillMaxWidth(0.72f).padding(vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spaces.large)
                    .padding(TvFocusDefaults.Reserve),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
            ) {
                Text(
                    text = torrent.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // ---- Magnet link -------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        MaterialTheme.spaces.small,
                    ),
                ) {
                    TvActionButton(
                        onClick = {
                            val magnetUri = (magnetLinkState as? MagnetLinkState.Ready)?.value
                            if (magnetUri != null) {
                                if (!context.openMagnetLink(magnetUri)) {
                                    onTorrentClientNotFound()
                                }
                            }
                        },
                        enabled = magnetLinkState is MagnetLinkState.Ready,
                    ) {
                        Text(text = stringResource(R.string.torrent_title_magnet_link))
                    }

                    TvActionButton(
                        onClick = {
                            viewModel.toggleBookmark(bookmark = !isBookmarked)
                        },
                    ) {
                        Text(
                            text = stringResource(
                                if (isBookmarked) {
                                    R.string.torrent_action_delete_bookmark
                                } else {
                                    R.string.torrent_action_bookmark_torrent
                                },
                            ),
                        )
                    }
                }

                Text(
                    text = stringResource(
                        when (magnetLinkState) {
                            MagnetLinkState.Loading ->
                                R.string.torrent_status_magnet_link_loading

                            MagnetLinkState.Error -> R.string.torrent_status_magnet_link_error
                            is MagnetLinkState.Ready ->
                                R.string.torrent_status_magnet_link_ready
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // ---- Torrent file ------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        MaterialTheme.spaces.small,
                    ),
                ) {
                    TvActionButton(
                        onClick = {
                            when (val fileState = torrentFileState) {
                                is TorrentFileState.LinkReady ->
                                    viewModel.downloadTorrentFile(fileState.value)

                                is TorrentFileState.DownloadComplete ->
                                    createTorrentFileLauncher.launch(fileState.fileName)

                                else -> Unit
                            }
                        },
                        enabled = torrentFileState is TorrentFileState.LinkReady ||
                            torrentFileState is TorrentFileState.DownloadComplete,
                    ) {
                        Text(
                            text = if (torrentFileState is TorrentFileState.DownloadComplete) {
                                stringResource(R.string.torrent_button_save_to_file)
                            } else {
                                stringResource(R.string.torrent_title_torrent_file)
                            },
                        )
                    }

                    TvActionButton(
                        onClick = {
                            val pageUrl = torrent.detailsPageUrl
                            if (pageUrl != null) {
                                onViewDetails(torrent.id, pageUrl, torrent.providerName)
                            }
                        },
                        enabled = torrent.detailsPageUrl != null,
                    ) {
                        Text(text = stringResource(R.string.torrent_title_details_page))
                    }
                }

                Text(
                    text = stringResource(torrentFileState.toMessageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TvActionButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.button_cancel))
                }
            }
        }
    }
}

private fun TorrentFileState.toMessageRes(): Int = when (this) {
    TorrentFileState.PreparingLink -> R.string.torrent_status_file_preparing_link
    TorrentFileState.WaitingForMagnetLink -> R.string.torrent_status_file_waiting_magnet_link
    TorrentFileState.LinkUnavailable -> R.string.torrent_status_unavailable
    is TorrentFileState.LinkReady -> R.string.torrent_status_file_link_ready
    TorrentFileState.Downloading -> R.string.torrent_status_file_downloading
    TorrentFileState.DownloadError -> R.string.torrent_status_file_download_failed
    TorrentFileState.FileNotFound -> R.string.torrent_status_file_not_found
    is TorrentFileState.DownloadComplete -> R.string.torrent_status_file_download_complete
    TorrentFileState.WritingContent -> R.string.torrent_status_file_saving
    TorrentFileState.ContentWriteComplete -> R.string.torrent_status_file_saved
}
