package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation

import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.extension.childComposable
import com.prajwalch.torrentsearch.ui.extension.parentComposable
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.AddEditSearchProviderScreen
import com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.CloudflareScreen

import kotlinx.serialization.Serializable

@Serializable
private object SearchProviders

@Serializable
private object Home

@Serializable
private data class AddEdit(val id: SearchProviderId? = null)

@Serializable
private data class Cloudflare(val id: SearchProviderId, val url: String)

fun NavGraphBuilder.searchProvidersNavigation(navController: NavHostController) {
    navigation<SearchProviders>(startDestination = Home) {
        parentComposable<Home> {
            SearchProvidersScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddSearchProvider = { navController.navigate(AddEdit()) },
                onNavigateToEditSearchProvider = { navController.navigate(AddEdit(id = it)) },
                onUnlockProtection = { id, url -> navController.navigate(Cloudflare(id, url)) }
            )
        }

        childComposable<AddEdit> {
            AddEditSearchProviderScreen(onNavigateBack = { navController.navigateUp() })
        }

        childComposable<Cloudflare> {
            CloudflareScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}

fun NavHostController.navigateToSearchProviders() {
    this.navigate(route = SearchProviders)
}