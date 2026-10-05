package com.prajwalch.torrentsearch.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.tv.home.TvHomeScreen
import com.prajwalch.torrentsearch.ui.tv.search.TvSearchScreen
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

/**
 * TV navigation graph.
 *
 * Mirrors the handheld `TorrentSearchApp` routes so every screen can reuse the
 * existing ViewModels unchanged, but with two differences:
 *
 * - The handheld predictive-back transitions are dropped. They exist to support
 *   the gesture back-swipe, which does not exist on a D-pad; the TV build uses
 *   a plain cross-fade + slide instead.
 * - BACK is handled by a single [BackHandler] at the graph root rather than by
 *   an unconditional `moveTaskToBack` in the Activity. Google TV quality
 *   criterion TV-DB requires BACK to unwind the stack and only then reach the
 *   TV home screen; when there is nothing left to pop we stop intercepting and
 *   let the platform handle it.
 */
@Composable
fun TorrentSearchTvApp() {
    val navController = rememberNavController()

    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.navigateUp()
    }

    TvNavHost(navController = navController)
}

private const val TRANSITION_MS = 220

@Composable
private fun TvNavHost(navController: NavHostController) {
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
            TvHomeScreen(
                onSearch = { query, category ->
                    navController.navigate(Search(query = query, category = category))
                },
            )
        }

        composable<Search> { entry ->
            val args = entry.toRoute<Search>()
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

        composable<TorrentDetails> { entry ->
            val args = entry.toRoute<TorrentDetails>()
            TvTorrentDetailsScreen()
        }
    }
}
