package com.prajwalch.torrentsearch.ui.settings.searchproviders.edit

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.TorznabSearchProviderConfigForm
import com.prajwalch.torrentsearch.ui.settings.searchproviders.TorznabSearchProviderConfigViewModel
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun EditSearchProviderScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<TorznabSearchProviderConfigViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isConfigSaved) {
        if (uiState.isConfigSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { EditSearchProviderScreenTopBar(onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        TorznabSearchProviderConfigForm(
            modifier = Modifier
                .verticalScroll(state = scrollState)
                .fillMaxWidth()
                .imePadding()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spaces.large),
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
                    Text(text = stringResource(R.string.button_update))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSearchProviderScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.edit_search_provider_screen_title)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_to_search_providers_screen,
            )
        },
    )
}