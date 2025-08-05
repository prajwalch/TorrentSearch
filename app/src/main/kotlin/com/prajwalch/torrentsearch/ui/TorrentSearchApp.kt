package com.prajwalch.torrentsearch.ui

import android.content.ClipData
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
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
import com.prajwalch.torrentsearch.ui.screens.settings.settingsNavigation
import com.prajwalch.torrentsearch.ui.viewmodel.BookmarksViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProvidersViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

import kotlinx.coroutines.launch

@Composable
fun TorrentSearchApp(
    searchViewModel: SearchViewModel,
    bookmarksViewModel: BookmarksViewModel,
    settingsViewModel: SettingsViewModel,
    searchProvidersViewModel: SearchProvidersViewModel,
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
            onDismissRequest = { selectedTorrent = null },
            title = torrent.name,
            onBookmarkTorrent = { bookmarksViewModel.bookmarkTorrent(torrent) },
            onDeleteBookmark = { bookmarksViewModel.deleteBookmarkedTorrent(torrent) },
            onDownloadTorrent = {
                isTorrentClientMissing = !onDownloadTorrent(torrent.magnetUri())
            },
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
            isNSFW = torrent.isNSFW(),
            isBookmarked = torrent.bookmarked,
            hasDescriptionPage = hasDescriptionPage,
        )
    }

    NavHost(navController = navController, startDestination = Screens.SEARCH) {
        composable(
            route = Screens.SEARCH,
            enterTransition = { fadeIn() },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
        ) {
            SearchScreen(
                onNavigateToBookmarks = { navController.navigate(Screens.BOOKMARKS) },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                viewModel = searchViewModel,
                onTorrentSelect = { selectedTorrent = it },
                snackbarHostState = snackbarHostState,
            )
        }

        composable(
            route = Screens.BOOKMARKS,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            BookmarksScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                viewModel = bookmarksViewModel,
                onTorrentSelect = { selectedTorrent = it },
                snackbarHostState = snackbarHostState,
            )
        }

        settingsNavigation(
            navController = navController,
            settingsViewModel = settingsViewModel,
            searchProvidersViewModel = searchProvidersViewModel,
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