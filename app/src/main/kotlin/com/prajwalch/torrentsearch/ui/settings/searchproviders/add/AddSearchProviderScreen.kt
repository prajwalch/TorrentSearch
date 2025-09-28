package com.prajwalch.torrentsearch.ui.settings.searchproviders.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.TextUrl
import com.prajwalch.torrentsearch.ui.components.TorznabSearchProviderConfigForm
import com.prajwalch.torrentsearch.ui.settings.searchproviders.TorznabSearchProviderConfigViewModel
import com.prajwalch.torrentsearch.ui.theme.spaces

private const val HOW_TO_WIKI_URL =
    "https://github.com/prajwalch/TorrentSearch/wiki/How-to-add-and-configure-Torznab-search-provider"

@Composable
fun AddSearchProviderScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<TorznabSearchProviderConfigViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isConfigSaved) {
        if (uiState.isConfigSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { AddSearchProviderScreenTopBar(onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .verticalScroll(state = scrollState)
                .fillMaxWidth()
                .imePadding()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spaces.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TorznabSearchProviderConfigForm(
                config = uiState.config,
                onNameChange = viewModel::changeName,
                onUrlChange = viewModel::changeUrl,
                onApiKeyChange = viewModel::changeAPIKey,
                onCategoryChange = viewModel::changeCategory,
                onSafetyStatusChange = viewModel::changeSafetyStatus,
                isUrlValid = uiState.isUrlValid,
                confirmButton = {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isConfigNotBlank(),
                        onClick = { viewModel.saveConfig() },
                    ) {
                        Text(text = stringResource(R.string.button_add))
                    }
                },
            )

            val uriHandler = LocalUriHandler.current
            TextUrl(
                text = stringResource(R.string.learn_how_to_add),
                onClick = { uriHandler.openUri(HOW_TO_WIKI_URL) },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSearchProviderScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.add_search_provider_screen_title)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_to_search_providers_screen,
            )
        },
    )
}