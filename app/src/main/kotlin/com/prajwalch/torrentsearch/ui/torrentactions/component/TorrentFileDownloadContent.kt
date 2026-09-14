package com.prajwalch.torrentsearch.ui.torrentactions.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.ui.torrentactions.TorrentFileState

import java.io.OutputStream

@Composable
fun TorrentFileDownloadContent(
    state: TorrentFileState,
    onWriteFileContent: (OutputStream) -> Unit,
    onCloseSheet: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentResolver = LocalContext.current.contentResolver
    val createTorrentFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(TorrentSearchConstants.MIME_TYPE_TORRENT),
    ) { fileUri ->
        fileUri
            ?.let(contentResolver::openOutputStream)
            ?.let(onWriteFileContent)
    }

    SideEffect(state) {
        if (state is TorrentFileState.DownloadComplete) {
            createTorrentFileLauncher.launch(state.fileName)
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = MaterialTheme.spaces.large)
            .padding(bottom = MaterialTheme.spaces.large)
            .verticalScroll(state = rememberScrollState())
            .animateContentSize(),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalIconButton(onClick = onGoBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }

            FilledTonalIconButton(onClick = onCloseSheet) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                )
            }
        }

        Crossfade(modifier = Modifier.height(400.dp), targetState = state) { targetState ->
            when (targetState) {
                is TorrentFileState.DownloadComplete -> {
                    FileDownloadCompleteState(
                        modifier = Modifier.fillMaxSize(),
                        onSaveFile = { createTorrentFileLauncher.launch(targetState.fileName) },
                    )
                }

                TorrentFileState.Downloading -> DownloadingState(Modifier.fillMaxSize())
                TorrentFileState.DownloadFailed -> DownloadFailedState(Modifier.fillMaxSize())
                TorrentFileState.FileNotFound -> FileNotFoundState(Modifier.fillMaxSize())
                TorrentFileState.WritingContent -> FileSavingState(Modifier.fillMaxSize())
                TorrentFileState.WriteComplete -> SaveCompleteState(Modifier.fillMaxSize())
            }
        }
    }
}


@Composable
private fun DownloadingState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = { CircularProgressIndicator() },
        title = { Text(stringResource(R.string.torrent_message_file_downloading)) },
    )
}

@Composable
private fun FileDownloadCompleteState(
    onSaveFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.SmallIconSize),
                painter = painterResource(R.drawable.ic_download_done),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(stringResource(R.string.torrent_message_file_download_complete)) },
        primaryAction = {
            Button(
                onClick = onSaveFile,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = null,
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.torrent_button_save_to_file))
            }
        },
    )
}

@Composable
private fun DownloadFailedState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.SmallIconSize),
                painter = painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.torrent_message_file_download_failed)) },
    )
}

@Composable
private fun FileNotFoundState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.SmallIconSize),
                painter = painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.torrent_message_file_not_found)) },
    )
}

@Composable
private fun FileSavingState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = { CircularProgressIndicator() },
        title = { Text(stringResource(R.string.torrent_message_file_saving)) },
    )
}

@Composable
private fun SaveCompleteState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.SmallIconSize),
                painter = painterResource(R.drawable.ic_folder_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(stringResource(R.string.torrent_message_file_saved)) },
    )
}