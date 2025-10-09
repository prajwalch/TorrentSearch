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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksScreen
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksViewModel
import com.prajwalch.torrentsearch.ui.components.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.search.SearchScreen
import com.prajwalch.torrentsearch.ui.searchhistory.SearchHistoryScreen
import com.prajwalch.torrentsearch.ui.searchresults.SearchResultsScreen
import com.prajwalch.torrentsearch.ui.settings.settingsNavigation

import kotlinx.coroutines.launch

@Composable
fun TorrentSearchApp(
    onDownloadTorrent: (MagnetUri) -> Boolean,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    startDestination: String = Screens.SEARCH,
) {
    val bookmarksViewModel = hiltViewModel<BookmarksViewModel>()

    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isTorrentClientMissing by remember { mutableStateOf(false) }
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    if (isTorrentClientMissing) {
        TorrentClientNotFoundDialog(
            onConfirmation = { isTorrentClientMissing = false },
        )
    }

    selectedTorrent?.let { torrent ->
        val clipboard = LocalClipboard.current

        val magnetLinkCopiedHint = stringResource(R.string.torrent_list_magnet_link_copied_msg)
        val descriptionPageUrlCopiedHint =
            stringResource(R.string.torrent_list_description_page_url_copied_msg)

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
            hasDescriptionPage = torrent.descriptionPageUrl.isNotEmpty(),
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(
            route = Screens.SEARCH,
            enterTransition = { fadeIn() },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
        ) {
            SearchScreen(
                onNavigateToBookmarks = { navController.navigate(Screens.BOOKMARKS) },
                onNavigateToSearchHistory = { navController.navigate(Screens.SEARCH_HISTORY) },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                onSearch = { query, category ->
                    navController.navigate(
                        Screens.createSearchResultsRoute(query = query, category = category)
                    )
                }
            )
        }

        composable(
            route = Screens.SEARCH_RESULTS,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            SearchResultsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                onResultClick = { selectedTorrent = it },
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
                onBookmarkClick = { selectedTorrent = it },
                snackbarHostState = snackbarHostState,
            )
        }

        composable(
            route = Screens.SEARCH_HISTORY,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            SearchHistoryScreen(onNavigateBack = { navController.navigateUp() })
        }

        settingsNavigation(navController = navController)
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