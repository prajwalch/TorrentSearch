package com.prajwalch.torrentsearch.ui.screens.settings.searchproviders

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.ui.screens.Screens

fun NavGraphBuilder.searchProvidersNavigation(navController: NavHostController) {
    navigation(
        startDestination = Screens.Settings.SearchProviders.HOME,
        route = Screens.Settings.SearchProviders.ROOT
    ) {
        composable(
            route = Screens.Settings.SearchProviders.HOME,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            SearchProvidersScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToNewProvider = {
                    navController.navigate(route = Screens.Settings.SearchProviders.NEW)
                },
                onNavigateToEditProvider = {
                    navController.navigate(
                        route = Screens.Settings.SearchProviders.createEditRoute(id = it)
                    )
                },
            )
        }

        composable(
            route = Screens.Settings.SearchProviders.NEW,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            NewSearchProviderScreen(onCancel = { navController.navigateUp() })
        }

        composable(
            route = Screens.Settings.SearchProviders.EDIT,
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            EditSearchProviderScreen(onCancel = { navController.navigateUp() })
        }
    }
}