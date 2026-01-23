package com.prajwalch.torrentsearch.ui.settings.searchproviders.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.TorznabConfigForm
import com.prajwalch.torrentsearch.ui.settings.searchproviders.TorznabConfigViewModel
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.utils.torznabConnectionCheckResultStringResource

@Composable
fun EditSearchProviderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorznabConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionCheckResultMessage = uiState.connectionCheckResult?.let {
        torznabConnectionCheckResultStringResource(result = it)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isConfigSaved) {
        if (uiState.isConfigSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(connectionCheckResultMessage) {
        if (connectionCheckResultMessage == null) return@LaunchedEffect

        snackbarHostState.showSnackbar(message = connectionCheckResultMessage)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        topBar = { EditSearchProviderScreenTopBar(onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            if (uiState.isConnectionCheckRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            TorznabConfigForm(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState())
                    .imePadding()
                    .padding(horizontal = MaterialTheme.spaces.large),
                searchProviderName = uiState.searchProviderName,
                onChangeSearchProviderName = viewModel::setSearchProviderName,
                url = uiState.url,
                onChangeUrl = viewModel::setUrl,
                apiKey = uiState.apiKey,
                onChangeApiKey = viewModel::setAPIKey,
                category = uiState.category,
                onChangeCategory = viewModel::setCategory,
                isUrlValid = uiState.isUrlValid,
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::checkConnection,
                            enabled = uiState.isConfigNotBlank(),
                        ) {
                            Text(text = stringResource(R.string.search_providers_button_check_connection))
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = uiState.isConfigNotBlank(),
                            onClick = { viewModel.saveConfig() },
                        ) {
                            Text(text = stringResource(R.string.search_providers_button_update))
                        }
                    }
                }
            )
        }
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
        title = { Text(text = stringResource(R.string.search_providers_edit_screen_title)) },
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
    )
}