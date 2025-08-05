package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.ui.screens.Screens
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProvidersViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

fun NavGraphBuilder.settingsNavigation(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    searchProvidersViewModel: SearchProvidersViewModel,
) {
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
                    navController.navigate(Screens.Settings.SEARCH_PROVIDERS)
                },
                onNavigateToDefaultSortOptions = {
                    navController.navigate(Screens.Settings.DEFAULT_SORT_OPTIONS)
                },
                viewModel = settingsViewModel,
            )
        }

        composable(
            route = Screens.Settings.DEFAULT_CATEGORY,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            DefaultCategoryScreen(
                onNavigateBack = { navController.navigateUp() },
                viewModel = settingsViewModel,
            )
        }

        composable(
            route = Screens.Settings.SEARCH_PROVIDERS,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            SearchProvidersScreen(
                onNavigateBack = { navController.navigateUp() },
                viewModel = searchProvidersViewModel,
            )
        }

        composable(
            route = Screens.Settings.DEFAULT_SORT_OPTIONS,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            DefaultSortOptionsScreen(
                onNavigateBack = { navController.navigateUp() },
                viewModel = settingsViewModel,
            )
        }
    }
}