package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constants.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.BottomInfo
import com.prajwalch.torrentsearch.ui.components.TextUrl
import com.prajwalch.torrentsearch.ui.components.TorznabConfigForm
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.utils.torznabConnectionCheckResultStringResource

@Composable
fun AddEditSearchProviderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorznabConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionCheckResultMessage = uiState.connectionCheckResult?.let {
        torznabConnectionCheckResultStringResource(it)
    }
    val topBarTitleId = if (uiState.isNewConfig) {
        R.string.search_providers_add_screen_title
    } else {
        R.string.search_providers_edit_screen_title
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isConfigSaved) {
        if (uiState.isConfigSaved) onNavigateBack()
    }

    LaunchedEffect(connectionCheckResultMessage) {
        if (connectionCheckResultMessage != null) {
            snackbarHostState.showSnackbar(message = connectionCheckResultMessage)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        topBar = {
            AddEditSearchProviderScreenTopBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(topBarTitleId),
            )
        },
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier.imePadding(),
                hostState = snackbarHostState,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            AnimatedVisibility(visible = uiState.isConnectionCheckRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = MaterialTheme.spaces.large)
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraLarge),
            ) {
                TorznabConfigForm(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState,
                    onChangeSearchProviderName = viewModel::setSearchProviderName,
                    onChangeUrl = viewModel::setUrl,
                    onChangeApiKey = viewModel::setAPIKey,
                    onChangeCategory = viewModel::setCategory,
                    onSaveConfig = viewModel::saveConfig,
                    onCheckConnection = viewModel::checkConnection,
                )

                LearnHowToAddLinkInfo()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSearchProviderScreenTopBar(
    onNavigateBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title) },
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
    )
}

@Composable
private fun TorznabConfigForm(
    uiState: TorznabConfigUiState,
    onChangeSearchProviderName: (String) -> Unit,
    onChangeUrl: (String) -> Unit,
    onChangeApiKey: (String) -> Unit,
    onChangeCategory: (Category) -> Unit,
    onSaveConfig: () -> Unit,
    onCheckConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TorznabConfigForm(
        modifier = modifier,
        searchProviderName = uiState.searchProviderName,
        onChangeSearchProviderName = onChangeSearchProviderName,
        url = uiState.url,
        onChangeUrl = onChangeUrl,
        apiKey = uiState.apiKey,
        onChangeApiKey = onChangeApiKey,
        category = uiState.category,
        onChangeCategory = onChangeCategory,
        isUrlValid = uiState.isUrlValid,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
            ) {
                CheckConnectionButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCheckConnection,
                    enabled = uiState.isConfigNotBlank() && !uiState.isConnectionCheckRunning,
                )
                SaveButton(
                    modifier = Modifier.weight(1f),
                    onClick = onSaveConfig,
                    enabled = uiState.isConfigNotBlank(),
                )
            }
        },
    )
}

@Composable
private fun CheckConnectionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(text = stringResource(R.string.search_providers_button_check_connection))
    }
}

@Composable
private fun SaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(text = stringResource(R.string.search_providers_button_save))
    }
}

@Composable
private fun LearnHowToAddLinkInfo(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    BottomInfo(modifier = modifier) {
        TextUrl(
            text = stringResource(R.string.search_providers_learn_how_to_add),
            onClick = { uriHandler.openUri(TorrentSearchConstants.TORZNAB_HOW_TO_ADD_WIKI) },
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}