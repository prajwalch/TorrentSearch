package com.prajwalch.torrentsearch.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.bookmarks.BookmarksScreen
import com.prajwalch.torrentsearch.ui.browse.BrowseScreen
import com.prajwalch.torrentsearch.ui.home.HomeScreen
import com.prajwalch.torrentsearch.ui.search.SearchScreen
import com.prajwalch.torrentsearch.ui.searchhistory.SearchHistoryScreen
import com.prajwalch.torrentsearch.ui.searchproviders.navigateToSearchProviders
import com.prajwalch.torrentsearch.ui.searchproviders.searchProvidersNavigation
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
    val id: String,
    val detailsPageUrl: String,
    val providerName: String,
)

@Serializable
private data class Browse(val category: Category = Category.All)

@Serializable
private object Bookmarks

@Serializable
private object SearchHistory

@Composable
fun TorrentSearchApp(initialSearchQuery: String? = null) {
    val activity = LocalActivity.current
    val navController = rememberNavController()
    val startDestination = initialSearchQuery?.let { Search(it) } ?: Home

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 3 }) +
                    fadeOut(targetAlpha = 0.6f)
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }) +
                    fadeIn(initialAlpha = 0.6f)
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
        },
        predictivePopEnterTransition = {
            slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }) +
                    fadeIn(initialAlpha = 0.6f)
        },
        predictivePopExitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
        }
    ) {
        composable<Home> {
            HomeScreen(
                onNavigateToBookmarks = { navController.navigate(Bookmarks) },
                onNavigateToSearchHistory = { navController.navigate(SearchHistory) },
                onNavigateToSettings = { navController.navigateToSettings() },
                onSearch = { query, category -> navController.navigate(Search(query, category)) },
                onBrowse = { category -> navController.navigate(Browse(category)) },
                onNavigateToSearchProviders = { navController.navigateToSearchProviders() },
            )
        }

        composable<Search> {
            SearchScreen(
                onNavigateBack = {
                    when (startDestination) {
                        is Home -> navController.navigateUp()
                        is Search -> activity?.finish()
                    }
                },
                onSearch = { query, category ->
                    // Replace the current search, so that back doesn't
                    // return to the previous results.
                    navController.navigate(Search(query, category)) {
                        popUpTo<Search> { inclusive = true }
                    }
                },
                onNavigateToSettings = { navController.navigateToSettings() },
                onNavigateToProviders = { navController.navigateToSearchProviders() },
                onNavigateToTorrentDetails = { id, pageUrl, providerName ->
                    navController.navigate(TorrentDetails(id, pageUrl, providerName))
                },
            )
        }

        composable<TorrentDetails> {
            TorrentDetailsScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable<Bookmarks> {
            BookmarksScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigateToSettings() },
                onNavigateToTorrentDetails = { id, pageUrl, providerName ->
                    navController.navigate(TorrentDetails(id, pageUrl, providerName))
                },
            )
        }

        composable<SearchHistory> {
            SearchHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onPerformSearch = {
                    navController.navigate(Search(query = it)) {
                        popUpTo(route = Home)
                    }
                },
            )
        }

        composable<Browse> {
            BrowseScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigateToSettings() },
                onNavigateToProviders = { navController.navigateToSearchProviders() },
                onNavigateToTorrentDetails = { id, pageUrl, providerName ->
                    navController.navigate(TorrentDetails(id, pageUrl, providerName))
                },
            )
        }

        searchProvidersNavigation(navController)
        settingsNavigation(navController)
    }
}