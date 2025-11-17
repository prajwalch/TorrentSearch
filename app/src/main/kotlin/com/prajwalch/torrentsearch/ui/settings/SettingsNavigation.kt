package com.prajwalch.torrentsearch.ui.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.extensions.childComposable
import com.prajwalch.torrentsearch.extensions.parentComposable
import com.prajwalch.torrentsearch.ui.Screens
import com.prajwalch.torrentsearch.ui.settings.defaultcategory.DefaultCategoryScreen
import com.prajwalch.torrentsearch.ui.settings.defaultsortoptions.DefaultSortOptionsScreen
import com.prajwalch.torrentsearch.ui.settings.searchproviders.searchProvidersNavigation

fun NavGraphBuilder.settingsNavigation(navController: NavHostController) {
    navigation(startDestination = Screens.Settings.MAIN, route = Screens.Settings.ROOT) {
        parentComposable(route = Screens.Settings.MAIN) {
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
            )
        }

        childComposable(route = Screens.Settings.DEFAULT_CATEGORY) {
            DefaultCategoryScreen(onNavigateBack = { navController.navigateUp() })
        }

        childComposable(route = Screens.Settings.DEFAULT_SORT_OPTIONS) {
            DefaultSortOptionsScreen(onNavigateBack = { navController.navigateUp() })
        }

        searchProvidersNavigation(navController = navController)
    }
}