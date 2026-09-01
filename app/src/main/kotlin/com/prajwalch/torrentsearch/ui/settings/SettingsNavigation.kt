package com.prajwalch.torrentsearch.ui.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

import com.prajwalch.torrentsearch.ui.searchproviders.navigateToSearchProviders
import com.prajwalch.torrentsearch.ui.settings.defaultsortoptions.DefaultSortOptionsScreen

import kotlinx.serialization.Serializable

@Serializable
private object Settings

@Serializable
private object DefaultSortOptions

fun NavGraphBuilder.settingsNavigation(navController: NavHostController) {
    composable<Settings> {
        SettingsScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSearchProviders = { navController.navigateToSearchProviders() },
            onNavigateToDefaultSortOptions = { navController.navigate(DefaultSortOptions) },
        )
    }

    composable<DefaultSortOptions> {
        DefaultSortOptionsScreen(onNavigateBack = { navController.navigateUp() })
    }
}

fun NavHostController.navigateToSettings() {
    this.navigate(route = Settings)
}