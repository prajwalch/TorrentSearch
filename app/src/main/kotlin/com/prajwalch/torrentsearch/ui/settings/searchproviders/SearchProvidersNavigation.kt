package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.extensions.childComposable
import com.prajwalch.torrentsearch.extensions.parentComposable
import com.prajwalch.torrentsearch.ui.Screens
import com.prajwalch.torrentsearch.ui.settings.searchproviders.add.AddSearchProviderScreen
import com.prajwalch.torrentsearch.ui.settings.searchproviders.edit.EditSearchProviderScreen

fun NavGraphBuilder.searchProvidersNavigation(navController: NavHostController) {
    navigation(
        startDestination = Screens.Settings.SearchProviders.HOME,
        route = Screens.Settings.SearchProviders.ROOT
    ) {
        parentComposable(route = Screens.Settings.SearchProviders.HOME) {
            SearchProvidersScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddSearchProvider = {
                    navController.navigate(route = Screens.Settings.SearchProviders.ADD)
                },
                onNavigateToEditSearchProvider = {
                    navController.navigate(
                        route = Screens.Settings.SearchProviders.createEditRoute(id = it)
                    )
                },
            )
        }

        childComposable(route = Screens.Settings.SearchProviders.ADD) {
            AddSearchProviderScreen(onNavigateBack = { navController.navigateUp() })
        }

        childComposable(route = Screens.Settings.SearchProviders.EDIT) {
            EditSearchProviderScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}