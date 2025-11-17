package com.prajwalch.torrentsearch.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.extensions.copyText
import com.prajwalch.torrentsearch.extensions.parentComposable
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksScreen
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
    viewModel: TorrentSearchAppViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTorrentClientNotFoundDialog by remember { mutableStateOf(false) }
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    if (showTorrentClientNotFoundDialog) {
        TorrentClientNotFoundDialog(
            onConfirmation = { showTorrentClientNotFoundDialog = false },
        )
    }

    selectedTorrent?.let { torrent ->
        val clipboard = LocalClipboard.current

        val magnetLinkCopiedMsg = stringResource(R.string.torrent_list_magnet_link_copied_message)
        val descriptionPageUrlCopiedMsg = stringResource(
            R.string.torrent_list_url_copied_message
        )

        TorrentActionsBottomSheet(
            onDismissRequest = { selectedTorrent = null },
            title = torrent.name,
            onBookmarkTorrent = { viewModel.bookmarkTorrent(torrent) },
            onDeleteBookmark = { viewModel.deleteBookmarkedTorrent(torrent) },
            onDownloadTorrent = {
                showTorrentClientNotFoundDialog = !onDownloadTorrent(torrent.magnetUri())
            },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.magnetUri())
                    snackbarHostState.showSnackbar(magnetLinkCopiedMsg)
                }
            },
            onShareMagnetLink = { onShareMagnetLink(torrent.magnetUri()) },
            onOpenDescriptionPage = { onOpenDescriptionPage(torrent.descriptionPageUrl) },
            onCopyDescriptionPageUrl = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.descriptionPageUrl)
                    snackbarHostState.showSnackbar(descriptionPageUrlCopiedMsg)
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

        parentComposable(route = Screens.SEARCH_RESULTS) {
            SearchResultsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                onResultClick = { selectedTorrent = it },
                snackbarHostState = snackbarHostState,
            )
        }

        parentComposable(route = Screens.BOOKMARKS) {
            BookmarksScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                onBookmarkClick = { selectedTorrent = it },
                snackbarHostState = snackbarHostState,
            )
        }

        parentComposable(route = Screens.SEARCH_HISTORY) {
            SearchHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onPerformSearch = {
                    val searchResultsRoute = Screens.createSearchResultsRoute(
                        query = it,
                        category = Category.All,
                    )
                    navController.navigate(searchResultsRoute) {
                        popUpTo(route = Screens.SEARCH)
                    }
                }
            )
        }

        settingsNavigation(navController = navController)
    }
}