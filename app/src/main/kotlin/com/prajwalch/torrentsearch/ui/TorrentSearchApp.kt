package com.prajwalch.torrentsearch.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.MagnetUri
import com.prajwalch.torrentsearch.extensions.parentComposable
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksScreen
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.home.HomeScreen
import com.prajwalch.torrentsearch.ui.search.SearchScreen
import com.prajwalch.torrentsearch.ui.searchhistory.SearchHistoryScreen
import com.prajwalch.torrentsearch.ui.settings.navigateToSettings
import com.prajwalch.torrentsearch.ui.settings.settingsNavigation

import kotlinx.serialization.Serializable

sealed interface TorrentSearchPrimaryRoute {
    @Serializable
    object Home : TorrentSearchPrimaryRoute

    @Serializable
    data class Search(
        val query: String,
        val category: Category? = null,
    ) : TorrentSearchPrimaryRoute

    @Serializable
    object Bookmarks : TorrentSearchPrimaryRoute

    @Serializable
    object SearchHistory : TorrentSearchPrimaryRoute
}

@Composable
fun TorrentSearchApp(
    onDownloadTorrent: (MagnetUri) -> Boolean,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    startDestination: TorrentSearchPrimaryRoute = TorrentSearchPrimaryRoute.Home,
) {
    val navController = rememberNavController()
    var showTorrentClientNotFoundDialog by rememberSaveable { mutableStateOf(false) }

    if (showTorrentClientNotFoundDialog) {
        TorrentClientNotFoundDialog(
            onConfirmation = { showTorrentClientNotFoundDialog = false },
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<TorrentSearchPrimaryRoute.Home>(
            enterTransition = { fadeIn() },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
        ) {
            HomeScreen(
                onNavigateToBookmarks = {
                    navController.navigate(TorrentSearchPrimaryRoute.Bookmarks)
                },
                onNavigateToSearchHistory = {
                    navController.navigate(TorrentSearchPrimaryRoute.SearchHistory)
                },
                onNavigateToSettings = { navController.navigateToSettings() },
                onSearch = { query, category ->
                    navController.navigate(TorrentSearchPrimaryRoute.Search(query, category))
                }
            )
        }

        parentComposable<TorrentSearchPrimaryRoute.Search> {
            SearchScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigateToSettings() },
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = onOpenDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        parentComposable<TorrentSearchPrimaryRoute.Bookmarks> {
            BookmarksScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigateToSettings() },
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = onOpenDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        parentComposable<TorrentSearchPrimaryRoute.SearchHistory> {
            SearchHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onPerformSearch = {
                    navController.navigate(TorrentSearchPrimaryRoute.Search(query = it)) {
                        popUpTo(route = TorrentSearchPrimaryRoute.Home)
                    }
                }
            )
        }

        settingsNavigation(navController = navController)
    }
}