package com.prajwalch.torrentsearch.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.TorrentFileDownloadEvent
import com.prajwalch.torrentsearch.domain.TorrentFileDownloadState

import kotlinx.coroutines.flow.Flow
import java.io.OutputStream

private const val TORRENT_FILE_MIME_TYPE = "application/x-bittorrent"

@Composable
fun TorrentFileDownloadEffect(
    onWrite: (OutputStream) -> Unit,
    state: TorrentFileDownloadState,
    events: Flow<TorrentFileDownloadEvent>,
    snackbarHostState: SnackbarHostState,
) {
    val contentResolver = LocalContext.current.contentResolver

    val createTorrentFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(TORRENT_FILE_MIME_TYPE),
    ) { fileUri ->
        fileUri?.let(contentResolver::openOutputStream)?.let(onWrite)
    }

    val fileDownloadingMessage = stringResource(R.string.torrent_file_downloading_message)
    val fileDownloadError = stringResource(R.string.torrent_file_download_error)
    val fileNotFoundError = stringResource(R.string.torrent_file_not_found_error)
    val fileSavingMessage = stringResource(R.string.torrent_file_saving_message)
    val fileSavedMessage = stringResource(R.string.torrent_file_saved_message)
    val fileSaveError = stringResource(R.string.torrent_file_save_error)

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is TorrentFileDownloadEvent.ReadyToWrite -> {
                    createTorrentFileLauncher.launch(event.fileName)
                }

                is TorrentFileDownloadEvent.DownloadFailed -> {
                    val message = event.message
                        ?.let { "$fileDownloadError: $it" }
                        ?: fileDownloadError

                    snackbarHostState.showSnackbar(message)
                }

                TorrentFileDownloadEvent.FileNotFound -> {
                    snackbarHostState.showSnackbar(fileNotFoundError)
                }

                TorrentFileDownloadEvent.WriteSucceed -> {
                    snackbarHostState.showSnackbar(fileSavedMessage)
                }

                is TorrentFileDownloadEvent.WriteFailed -> {
                    val message = event.message
                        ?.let { "$fileSaveError: $it" }
                        ?: fileSaveError

                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    LaunchedEffect(state) {
        when (state) {
            TorrentFileDownloadState.Idle -> {
                /* Do nothing */
            }

            TorrentFileDownloadState.Downloading -> {
                snackbarHostState.showSnackbar(
                    message = fileDownloadingMessage,
                    duration = SnackbarDuration.Indefinite,
                )
            }

            TorrentFileDownloadState.Writing -> {
                snackbarHostState.showSnackbar(
                    message = fileSavingMessage,
                    duration = SnackbarDuration.Indefinite,
                )
            }
        }
    }
}