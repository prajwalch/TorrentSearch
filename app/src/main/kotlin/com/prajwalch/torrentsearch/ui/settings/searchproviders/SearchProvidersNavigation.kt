package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.extensions.childComposable
import com.prajwalch.torrentsearch.extensions.parentComposable
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.settings.searchproviders.add.AddSearchProviderScreen
import com.prajwalch.torrentsearch.ui.settings.searchproviders.edit.EditSearchProviderScreen

import kotlinx.serialization.Serializable

@Serializable
private object SearchProviders

@Serializable
private object Home

@Serializable
private object Add

@Serializable
private data class Edit(val id: SearchProviderId)

fun NavGraphBuilder.searchProvidersNavigation(navController: NavHostController) {
    navigation<SearchProviders>(startDestination = Home) {
        parentComposable<Home> {
            SearchProvidersScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddSearchProvider = { navController.navigate(Add) },
                onNavigateToEditSearchProvider = { navController.navigate(Edit(id = it)) },
            )
        }

        childComposable<Add> {
            AddSearchProviderScreen(onNavigateBack = { navController.navigateUp() })
        }

        childComposable<Edit> {
            EditSearchProviderScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}

fun NavHostController.navigateToSearchProviders() {
    this.navigate(route = SearchProviders)
}