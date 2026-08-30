package com.prajwalch.torrentsearch.ui.searchproviders

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.searchproviders.addedit.AddEditSearchProviderScreen

import kotlinx.serialization.Serializable

@Serializable
private object SearchProviders

@Serializable
private object SearchProviderList

@Serializable
private data class AddEdit(val id: SearchProviderId? = null)

fun NavGraphBuilder.searchProvidersNavigation(navController: NavHostController) {
    navigation<SearchProviders>(startDestination = SearchProviderList) {
        composable<SearchProviderList> {
            SearchProvidersScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddSearchProvider = { navController.navigate(AddEdit()) },
                onNavigateToEditSearchProvider = { navController.navigate(AddEdit(id = it)) }
            )
        }

        composable<AddEdit> {
            AddEditSearchProviderScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}

fun NavHostController.navigateToSearchProviders() {
    this.navigate(route = SearchProviders)
}