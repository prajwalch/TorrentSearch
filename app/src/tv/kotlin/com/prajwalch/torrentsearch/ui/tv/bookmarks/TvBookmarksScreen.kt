package com.prajwalch.torrentsearch.ui.tv.bookmarks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksState
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksViewModel
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvChip
import com.prajwalch.torrentsearch.ui.tv.component.TvChipRow
import com.prajwalch.torrentsearch.ui.tv.component.TvFocusDefaults
import com.prajwalch.torrentsearch.ui.tv.component.TvMessageState
import com.prajwalch.torrentsearch.ui.tv.component.TvTorrentCard
import com.prajwalch.torrentsearch.ui.tv.component.TvTopBar
import com.prajwalch.torrentsearch.ui.tv.torrentactions.TvTorrentActionsDialog
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding

import com.prajwalch.torrentsearch.ui.tv.settings.TvConfirmDialog

import org.koin.androidx.compose.koinViewModel

/**
 * TV bookmarks.
 *
 * The handheld screen deletes a bookmark by swiping left. There is no D-pad
 * equivalent, so here OK opens the torrent action dialog - which already offers
 * "Delete bookmark" - and there is no separate delete affordance. The
 * swipe-to-delete tip is deliberately not shown.
 */
@Composable
fun TvBookmarksScreen(
    onNavigateToTorrentDetails: (id: String, pageUrl: String, providerName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use(viewModel::importBookmarks)
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TorrentSearchConstants.BOOKMARKS_EXPORT_FILE_TYPE),
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.use(viewModel::exportBookmarks)
            }
        }
    }

    // Screen entry point: focus lands on the top-bar action when this route opens.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(
            title = stringResource(R.string.bookmarks_screen_title),
            subtitle = uiState.totalBookmarksCount.toString(),
            actions = {
                TvActionButton(
                    onClick = { showDeleteAllConfirm = true },
                    modifier = entryFocusRequester
                        ?.let { Modifier.focusRequester(it) } ?: Modifier,
                ) {
                    Text(text = stringResource(R.string.bookmarks_action_delete_all))
                }
                TvActionButton(
                    onClick = { exportLauncher.launch(TorrentSearchConstants.BOOKMARKS_EXPORT_FILE_NAME) },
                ) {
                    Text(text = stringResource(R.string.bookmarks_action_export))
                }
                TvActionButton(onClick = { importLauncher.launch("*/*") }) {
                    Text(text = stringResource(R.string.bookmarks_action_import))
                }
            },
        )

        when (val state = uiState.bookmarksState) {
            BookmarksState.Loading -> Unit

            BookmarksState.Empty -> TvMessageState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.bookmarks_empty_message),
            )

            BookmarksState.EmptyNoMatches -> TvMessageState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.bookmarks_no_bookmarks_matched),
            )

            is BookmarksState.Ready -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = tvListContentPadding(),
                verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
            ) {
                items(state.bookmarks, key = { it.id }) { torrent ->
                    TvTorrentCard(
                        torrent = torrent,
                        onClick = { selectedTorrent = torrent },
                    )
                }
            }
        }
    }

    selectedTorrent?.let { torrent ->
        TvTorrentActionsDialog(
            torrent = torrent,
            onDismiss = { selectedTorrent = null },
            onViewDetails = { id, pageUrl, providerName ->
                selectedTorrent = null
                onNavigateToTorrentDetails(id, pageUrl, providerName)
            },
        )
    }

    if (showDeleteAllConfirm) {
        TvConfirmDialog(
            title = stringResource(R.string.bookmarks_dialog_title_delete_all),
            message = stringResource(R.string.bookmarks_dialog_message_delete_all),
            confirmLabel = stringResource(R.string.bookmarks_button_delete),
            onConfirm = {
                viewModel.deleteAllBookmarks()
                showDeleteAllConfirm = false
            },
            onDismiss = { showDeleteAllConfirm = false },
        )
    }
}