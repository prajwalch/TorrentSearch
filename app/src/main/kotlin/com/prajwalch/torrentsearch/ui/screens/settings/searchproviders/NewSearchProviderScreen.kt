package com.prajwalch.torrentsearch.ui.screens.settings.searchproviders

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.TorznabSearchProviderConfigForm
import com.prajwalch.torrentsearch.ui.viewmodel.TorznabSearchProviderConfigViewModel

@Composable
fun NewSearchProviderScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<TorznabSearchProviderConfigViewModel>()
    val config by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { NewSearchProviderScreenTopBar(onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        TorznabSearchProviderConfigForm(
            modifier = Modifier
                .consumeWindowInsets(innerPadding)
                .fillMaxWidth(),
            config = config,
            onNameChange = viewModel::changeName,
            onUrlChange = viewModel::changeUrl,
            onApiKeyChange = viewModel::changeAPIKey,
            onCategoryChange = viewModel::changeCategory,
            onSafetyStatusChange = viewModel::changeSafetyStatus,
            onSave = {
                viewModel.save()
                onNavigateBack()
            },
            contentPadding = innerPadding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSearchProviderScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.new_search_provider_screen_title)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_to_search_providers_screen,
            )
        },
    )
}