package com.prajwalch.torrentsearch.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksScreen
import com.prajwalch.torrentsearch.ui.browse.BrowseScreen
import com.prajwalch.torrentsearch.ui.component.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.extension.childComposable
import com.prajwalch.torrentsearch.ui.extension.parentComposable
import com.prajwalch.torrentsearch.ui.home.HomeScreen
import com.prajwalch.torrentsearch.ui.search.SearchScreen
import com.prajwalch.torrentsearch.ui.searchhistory.SearchHistoryScreen
import com.prajwalch.torrentsearch.ui.settings.navigateToSettings
import com.prajwalch.torrentsearch.ui.settings.settingsNavigation
import com.prajwalch.torrentsearch.ui.torrentdetails.TorrentDetailsScreen

import kotlinx.serialization.Serializable

@Serializable
private object Home

@Serializable
private data class Search(
    val query: String,
    val category: Category = Category.All,
)

@Serializable
private data class TorrentDetails(
    val detailsPageUrl: String,
    val providerName: String,
)

@Serializable
private object Browse

@Serializable
private object Bookmarks

@Serializable
private object SearchHistory

@Composable
fun TorrentSearchApp(
    onDownloadTorrent: (MagnetUri) -> Boolean,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    initialSearchQuery: String? = null,
    openTorrentDetailsInApp: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current
    val navController = rememberNavController()

    val openDescriptionPage = { url: String, providerName: String ->
        if (!openTorrentDetailsInApp) {
            uriHandler.openUri(url)
        } else {
            val detailsRoute = TorrentDetails(
                detailsPageUrl = url,
                providerName = providerName,
            )
            navController.navigate(detailsRoute)
        }
    }

    var showTorrentClientNotFoundDialog by rememberSaveable { mutableStateOf(false) }
    if (showTorrentClientNotFoundDialog) {
        TorrentClientNotFoundDialog(
            onConfirmation = { showTorrentClientNotFoundDialog = false },
        )
    }

    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery == null) return@LaunchedEffect

        val searchRoute = Search(query = initialSearchQuery)
        navController.navigate(searchRoute) {
            popUpTo(Home) {
                // When the initial query is given, going back should close
                // the app instead of showing or navigating to home screen.
                inclusive = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Home) {
        composable<Home>(
            enterTransition = { fadeIn() },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
        ) {
            HomeScreen(
                onNavigateToBookmarks = { navController.navigate(Bookmarks) },
                onNavigateToSearchHistory = { navController.navigate(SearchHistory) },
                onNavigateToSettings = { navController.navigateToSettings() },
                onSearch = { query, category -> navController.navigate(Search(query, category)) },
                onBrowse = { navController.navigate(Browse) },
            )
        }

        parentComposable<Search> {
            SearchScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigateToSettings() },
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = openDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        childComposable<TorrentDetails> {
            TorrentDetailsScreen(
                onNavigateBack = navController::navigateUp,
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareDetailsPageLink = onShareDescriptionPageUrl,
            )
        }

        parentComposable<Bookmarks> {
            BookmarksScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigateToSettings() },
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = openDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        parentComposable<SearchHistory> {
            SearchHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onPerformSearch = {
                    navController.navigate(Search(query = it)) {
                        popUpTo(route = Home)
                    }
                }
            )
        }

        parentComposable<Browse> {
            BrowseScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigateToSettings() },
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = openDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        settingsNavigation(navController = navController)
    }
}