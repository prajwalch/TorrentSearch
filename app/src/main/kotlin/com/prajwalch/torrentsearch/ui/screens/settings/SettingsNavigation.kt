package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.ui.screens.Screens
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

fun NavGraphBuilder.settingsNavigation(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
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
                onNavigateToCategoryList = {
                    navController.navigate(Screens.Settings.CATEGORY_LIST)
                },
                onNavigateToProvidersSetting = {
                    navController.navigate(Screens.Settings.SEARCH_PROVIDERS)
                },
                viewModel = settingsViewModel,
            )
        }

        composable(
            route = Screens.Settings.CATEGORY_LIST,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            CategoryListScreen(
                onNavigateBack = { navController.navigateUp() },
                viewModel = settingsViewModel,
            )
        }

        composable(
            route = Screens.Settings.SEARCH_PROVIDERS,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            SearchProvidersSetting(
                onNavigateBack = { navController.navigateUp() },
                viewModel = settingsViewModel,
            )
        }
    }
}