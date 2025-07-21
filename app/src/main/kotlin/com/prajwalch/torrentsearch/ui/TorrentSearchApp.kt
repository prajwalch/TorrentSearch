package com.prajwalch.torrentsearch.ui

import android.content.ClipData
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.screens.BookmarksScreen
import com.prajwalch.torrentsearch.ui.screens.Screens
import com.prajwalch.torrentsearch.ui.screens.SearchScreen
import com.prajwalch.torrentsearch.ui.screens.settings.SettingsScreen
import com.prajwalch.torrentsearch.ui.viewmodel.BookmarksViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

import kotlinx.coroutines.launch

@Composable
fun TorrentSearchApp(
    searchViewModel: SearchViewModel,
    bookmarksViewModel: BookmarksViewModel,
    settingsViewModel: SettingsViewModel,
    onDownloadTorrent: (MagnetUri) -> Boolean,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val clipboard = LocalClipboard.current

    var isTorrentClientMissing by remember { mutableStateOf(false) }
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    if (isTorrentClientMissing) {
        TorrentClientNotFoundDialog(
            onConfirmation = { isTorrentClientMissing = false },
        )
    }

    selectedTorrent?.let { torrent ->
        val magnetLinkCopiedHint = stringResource(R.string.hint_magnet_link_copied)
        val descriptionPageUrlCopiedHint = stringResource(R.string.hint_description_page_url_copied)
        val hasDescriptionPage = torrent.descriptionPageUrl.isNotEmpty()

        TorrentActionsBottomSheet(
            title = torrent.name,
            isNSFW = torrent.category?.isNSFW ?: true,
            onDismissRequest = { selectedTorrent = null },
            isBookmarked = torrent.bookmarked,
            onBookmark = { bookmarksViewModel.add(torrent) },
            onDeleteBookmark = { bookmarksViewModel.delete(torrent) },
            onDownloadTorrent = { onDownloadTorrent(torrent.magnetUri()) },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.magnetUri())
                    snackbarHostState.showSnackbar(magnetLinkCopiedHint)
                }
            },
            onShareMagnetLink = { onShareMagnetLink(torrent.magnetUri()) },
            onOpenDescriptionPage = { onOpenDescriptionPage(torrent.descriptionPageUrl) },
            onCopyDescriptionPageUrl = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.descriptionPageUrl)
                    snackbarHostState.showSnackbar(descriptionPageUrlCopiedHint)
                }
            },
            onShareDescriptionPageUrl = { onShareDescriptionPageUrl(torrent.descriptionPageUrl) },
            hasDescriptionPage = hasDescriptionPage,
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screens.SEARCH,
        enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
        exitTransition = { slideOutHorizontally { fullWidth -> fullWidth } },
    ) {
        composable(
            route = Screens.SEARCH,
            enterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
            exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } }
        ) {
            SearchScreen(
                onNavigateToBookmarks = { navController.navigate(Screens.BOOKMARKS) },
                onNavigateToSettings = { navController.navigate(Screens.SETTINGS) },
                viewModel = searchViewModel,
                onTorrentSelect = { selectedTorrent = it },
                snackbarHostState = snackbarHostState,
            )
        }

        composable(route = Screens.BOOKMARKS) {
            BookmarksScreen(
                onNavigateBack = { navController.navigateUp() },
                viewModel = bookmarksViewModel,
                onTorrentSelect = { selectedTorrent = it },
                snackbarHostState = snackbarHostState,
            )
        }

        composable(route = Screens.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                viewModel = settingsViewModel,
            )
        }
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