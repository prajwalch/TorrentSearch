package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.ui.screens.Screens
import com.prajwalch.torrentsearch.ui.screens.settings.searchproviders.searchProvidersNavigation

fun NavGraphBuilder.settingsNavigation(navController: NavHostController) {
    navigation(startDestination = Screens.Settings.MAIN, route = Screens.Settings.ROOT) {
        composable(
            route = Screens.Settings.MAIN,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDefaultCategory = {
                    navController.navigate(Screens.Settings.DEFAULT_CATEGORY)
                },
                onNavigateToSearchProviders = {
                    navController.navigate(Screens.Settings.SearchProviders.ROOT)
                },
                onNavigateToDefaultSortOptions = {
                    navController.navigate(Screens.Settings.DEFAULT_SORT_OPTIONS)
                },
                onNavigateToSearchHistory = {
                    navController.navigate(Screens.Settings.SEARCH_HISTORY)
                },
            )
        }

        composable(
            route = Screens.Settings.DEFAULT_CATEGORY,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            DefaultCategoryScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screens.Settings.DEFAULT_SORT_OPTIONS,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            DefaultSortOptionsScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screens.Settings.SEARCH_HISTORY,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            SearchHistoryScreen(onNavigateBack = { navController.navigateUp() })
        }

        searchProvidersNavigation(navController = navController)
    }
}