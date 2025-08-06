package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

val LocalSettingsViewModel = compositionLocalOf<SettingsViewModel> {
    error("Settings ViewModel is not provided")
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDefaultCategory: () -> Unit,
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToDefaultSortOptions: () -> Unit,
    onNavigateToSearchHistory: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { SettingsScreenTopBar(onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        CompositionLocalProvider(LocalSettingsViewModel provides viewModel) {
            LazyColumn(
                modifier = Modifier.consumeWindowInsets(innerPadding),
                contentPadding = innerPadding,
            ) {
                item { AppearanceSettings() }
                item { GeneralSettings(onNavigateToDefaultCategory = onNavigateToDefaultCategory) }
                item {
                    SearchSettings(
                        onNavigateToSearchProviders = onNavigateToSearchProviders,
                        onNavigateToDefaultSortOptions = onNavigateToDefaultSortOptions,
                    )
                }
                item {
                    SearchHistorySettings(onNavigateToSearchHistory = onNavigateToSearchHistory)
                }
                item { About() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenTopBar(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.settings_screen_title)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_back_to_search_screen,
            )
        }
    )
}