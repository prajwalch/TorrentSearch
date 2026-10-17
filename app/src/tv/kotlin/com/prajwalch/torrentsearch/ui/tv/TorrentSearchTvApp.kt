package com.prajwalch.torrentsearch.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.tv.bookmarks.TvBookmarksScreen
import com.prajwalch.torrentsearch.ui.tv.browse.TvBrowseScreen
import com.prajwalch.torrentsearch.ui.tv.component.TvDestination
import com.prajwalch.torrentsearch.ui.tv.component.TvScaffold
import com.prajwalch.torrentsearch.ui.tv.home.TvHomeScreen
import com.prajwalch.torrentsearch.ui.tv.search.TvSearchScreen
import com.prajwalch.torrentsearch.ui.tv.searchhistory.TvSearchHistoryScreen
import com.prajwalch.torrentsearch.ui.tv.searchproviders.TvSearchProvidersScreen
import com.prajwalch.torrentsearch.ui.tv.settings.TvSettingsScreen
import com.prajwalch.torrentsearch.ui.tv.settings.defaultsortoptions.TvDefaultSortOptionsScreen
import com.prajwalch.torrentsearch.ui.tv.torrentdetails.TvTorrentDetailsScreen

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
private object Browse

@Serializable
private object Bookmarks

@Serializable
private object SearchHistory

@Serializable
private object Providers

@Serializable
private object Settings

@Serializable
private object DefaultSortOptions

private const val TRANSITION_MS = 220

/**
 * TV navigation graph.
 *
 * Mirrors the handheld routes so every screen can reuse the existing ViewModels,
 * but every destination is now also reachable from the persistent left menu rail -
 * the handheld app hides most of these behind small top-bar icons, which a D-pad
 * makes very hard to find.
 *
 * BACK unwinds the stack and only then reaches the TV home screen (criterion
 * TV-DB). Switching section from the rail resets to that section's root rather than
 * stacking up destinations, so BACK from a freshly picked menu item returns to
 * wherever the user came from instead of walking back through every section.
 */
@Composable
fun TorrentSearchTvApp() {
    val navController = rememberNavController()

    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.navigateUp()
    }

    // The active section is tracked explicitly. Deriving it from
    // `currentDestination?.route` compares a route *object*'s toString() against a
    // route value, which is not a reliable equality check for typed routes.
    var currentDestination by rememberSaveable { mutableStateOf(TvDestination.Search) }

    val goTo: (TvDestination) -> Unit = { destination ->
        if (destination != currentDestination) {
            currentDestination = destination
            val route: Any = when (destination) {
                TvDestination.Search -> Home
                TvDestination.Browse -> Browse
                TvDestination.Bookmarks -> Bookmarks
                TvDestination.History -> SearchHistory
                TvDestination.Providers -> Providers
                TvDestination.Settings -> Settings
            }
            navController.navigate(route) {
                // Switching sections returns to that section's root, so BACK goes
                // back to where the user came from instead of walking through every
                // section they have visited.
                popUpTo<Home> { inclusive = route == Home }
                launchSingleTop = true
            }
        }
    }

    TvNavHost(navController = navController, onNavigate = goTo)
}

@Composable
private fun TvNavHost(
    navController: NavHostController,
    onNavigate: (TvDestination) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        enterTransition = { fadeIn(tween(TRANSITION_MS)) },
        exitTransition = {
            slideOutHorizontally(tween(TRANSITION_MS)) { -it / 4 } + fadeOut(tween(TRANSITION_MS))
        },
        popEnterTransition = {
            slideInHorizontally(tween(TRANSITION_MS)) { -it / 4 } + fadeIn(tween(TRANSITION_MS))
        },
        popExitTransition = { fadeOut(tween(TRANSITION_MS)) },
    ) {
        composable<Home> {
            TvScaffold(TvDestination.Search, onNavigate) {
                TvHomeScreen(
                    onSearch = { query, category ->
                        navController.navigate(Search(query = query, category = category))
                    },
                )
            }
        }

        composable<Search> { entry ->
            val args = entry.toRoute<Search>()
            TvScaffold(TvDestination.Search, onNavigate) {
                TvSearchScreen(
                    query = args.query,
                    category = args.category,
                    onNavigateToTorrentDetails = { id, pageUrl, providerName ->
                        navController.navigate(
                            TorrentDetails(
                                id = id,
                                detailsPageUrl = pageUrl,
                                providerName = providerName,
                            ),
                        )
                    },
                )
            }
        }

        composable<TorrentDetails> {
            TvScaffold(TvDestination.Search, onNavigate) {
                TvTorrentDetailsScreen()
            }
        }

        composable<Browse> {
            TvScaffold(TvDestination.Browse, onNavigate) {
                TvBrowseScreen(
                    onNavigateToSearchProviders = { onNavigate(TvDestination.Providers) },
                    onNavigateToTorrentDetails = { id, pageUrl, providerName ->
                        navController.navigate(
                            TorrentDetails(id, pageUrl, providerName),
                        )
                    },
                )
            }
        }

        composable<Bookmarks> {
            TvScaffold(TvDestination.Bookmarks, onNavigate) {
                TvBookmarksScreen(
                    onNavigateToTorrentDetails = { id, pageUrl, providerName ->
                        navController.navigate(
                            TorrentDetails(id, pageUrl, providerName),
                        )
                    },
                )
            }
        }

        composable<SearchHistory> {
            TvScaffold(TvDestination.History, onNavigate) {
                TvSearchHistoryScreen(
                    onPerformSearch = { query ->
                        navController.navigate(Search(query = query)) {
                            popUpTo<Home> { }
                        }
                    },
                )
            }
        }

        composable<Providers> {
            TvScaffold(TvDestination.Providers, onNavigate) {
                TvSearchProvidersScreen()
            }
        }

        composable<Settings> {
            TvScaffold(TvDestination.Settings, onNavigate) {
                TvSettingsScreen(
                    onNavigateToSearchProviders = { onNavigate(TvDestination.Providers) },
                    onNavigateToDefaultSortOptions = {
                        navController.navigate(DefaultSortOptions)
                    },
                )
            }
        }

        composable<DefaultSortOptions> {
            TvScaffold(TvDestination.Settings, onNavigate) {
                TvDefaultSortOptionsScreen()
            }
        }
    }
}