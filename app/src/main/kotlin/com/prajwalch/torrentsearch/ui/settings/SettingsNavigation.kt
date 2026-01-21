package com.prajwalch.torrentsearch.ui.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.extensions.childComposable
import com.prajwalch.torrentsearch.extensions.parentComposable
import com.prajwalch.torrentsearch.ui.settings.defaultcategory.DefaultCategoryScreen
import com.prajwalch.torrentsearch.ui.settings.defaultsortoptions.DefaultSortOptionsScreen
import com.prajwalch.torrentsearch.ui.settings.searchproviders.navigateToSearchProviders
import com.prajwalch.torrentsearch.ui.settings.searchproviders.searchProvidersNavigation

import kotlinx.serialization.Serializable

@Serializable
private object Settings

@Serializable
private object Home

@Serializable
private object DefaultCategory

@Serializable
private object DefaultSortOptions

fun NavGraphBuilder.settingsNavigation(navController: NavHostController) {
    navigation<Settings>(startDestination = Home) {
        parentComposable<Home> {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDefaultCategory = { navController.navigate(DefaultCategory) },
                onNavigateToSearchProviders = { navController.navigateToSearchProviders() },
                onNavigateToDefaultSortOptions = { navController.navigate(DefaultSortOptions) },
            )
        }

        childComposable<DefaultCategory> {
            DefaultCategoryScreen(onNavigateBack = { navController.navigateUp() })
        }

        childComposable<DefaultSortOptions> {
            DefaultSortOptionsScreen(onNavigateBack = { navController.navigateUp() })
        }

        searchProvidersNavigation(navController = navController)
    }
}

fun NavHostController.navigateToSettings() {
    this.navigate(route = Settings)
}