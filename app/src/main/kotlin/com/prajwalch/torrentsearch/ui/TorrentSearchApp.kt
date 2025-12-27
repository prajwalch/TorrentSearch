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

import com.prajwalch.torrentsearch.extensions.parentComposable
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.MagnetUri
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksScreen
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.home.HomeScreen
import com.prajwalch.torrentsearch.ui.search.SearchScreen
import com.prajwalch.torrentsearch.ui.searchhistory.SearchHistoryScreen
import com.prajwalch.torrentsearch.ui.settings.settingsNavigation

@Composable
fun TorrentSearchApp(
    onDownloadTorrent: (MagnetUri) -> Boolean,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    startDestination: String = Screens.HOME,
) {
    val navController = rememberNavController()
    var showTorrentClientNotFoundDialog by rememberSaveable { mutableStateOf(false) }

    if (showTorrentClientNotFoundDialog) {
        TorrentClientNotFoundDialog(
            onConfirmation = { showTorrentClientNotFoundDialog = false },
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(
            route = Screens.HOME,
            enterTransition = { fadeIn() },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
        ) {
            HomeScreen(
                onNavigateToBookmarks = { navController.navigate(Screens.BOOKMARKS) },
                onNavigateToSearchHistory = { navController.navigate(Screens.SEARCH_HISTORY) },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                onSearch = { query, category ->
                    navController.navigate(
                        Screens.createSearchRoute(query = query, category = category)
                    )
                }
            )
        }

        parentComposable(route = Screens.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = onOpenDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        parentComposable(route = Screens.BOOKMARKS) {
            BookmarksScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(Screens.Settings.ROOT) },
                onDownloadTorrent = { showTorrentClientNotFoundDialog = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = onOpenDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        parentComposable(route = Screens.SEARCH_HISTORY) {
            SearchHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onPerformSearch = {
                    val searchRoute = Screens.createSearchRoute(
                        query = it,
                        category = Category.All,
                    )
                    navController.navigate(searchRoute) {
                        popUpTo(route = Screens.HOME)
                    }
                }
            )
        }

        settingsNavigation(navController = navController)
    }
}