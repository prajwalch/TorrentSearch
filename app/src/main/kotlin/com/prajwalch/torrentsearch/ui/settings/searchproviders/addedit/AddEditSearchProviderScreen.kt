package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit

import android.content.res.Resources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.TorznabConnectionCheckResult
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component.AutoDetectCategoriesButton
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component.CheckConnectionButton
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component.LearnHowToAddLinkInfo
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component.OutlinedUrlTextField
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component.SaveButton
import com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component.SupportedCategories
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun AddEditSearchProviderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorznabConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarTitleId = if (uiState.isNewConfig) {
        R.string.search_providers_add_screen_title
    } else {
        R.string.search_providers_edit_screen_title
    }

    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TorznabConfigEvent.ConfigSaved -> onNavigateBack()

                is TorznabConfigEvent.ConnectionCheckCompleted -> {
                    snackbarHostState.showSnackbar(event.result.displayName(resources))
                }
            }
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
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.imePadding(),
                actions = {},
                floatingActionButton = {
                    SaveButton(
                        onClick = viewModel::saveConfig,
                        enabled = uiState.isConfigNotBlank(),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    onCheckConnection = viewModel::checkConnection,
                    onToggleCategorySelection = viewModel::toggleCategorySelection,
                    onAutoDetectCategories = viewModel::detectSupportedCategories,
                )

                LearnHowToAddLinkInfo(
                    modifier = Modifier.padding(vertical = MaterialTheme.spaces.large),
                )
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
    onCheckConnection: () -> Unit,
    onToggleCategorySelection: (Category) -> Unit,
    onAutoDetectCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        Card {
            Column(
                modifier = Modifier.padding(MaterialTheme.spaces.large),
                verticalArrangement = Arrangement.spacedBy(space = MaterialTheme.spaces.small),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.searchProviderName,
                    onValueChange = onChangeSearchProviderName,
                    label = { Text(stringResource(R.string.search_providers_label_name)) },
                    singleLine = true,
                )
                OutlinedUrlTextField(
                    modifier = Modifier.fillMaxWidth(),
                    url = uiState.url,
                    onUrlChange = onChangeUrl,
                    isError = !uiState.isUrlValid,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.apiKey,
                    onValueChange = onChangeApiKey,
                    label = { Text(stringResource(R.string.search_providers_label_api_key)) },
                    singleLine = true,
                )
                CheckConnectionButton(
                    onClick = onCheckConnection,
                    enabled = uiState.isConfigNotBlank() && !uiState.isCheckingConnection,
                    isCheckingConnection = uiState.isCheckingConnection,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spaces.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.search_providers_title_supported_categories),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.search_providers_subtitle_supported_categories),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                SupportedCategories(
                    supportedCategories = uiState.supportedCategories,
                    onToggleCategorySelection = onToggleCategorySelection,
                )
                AutoDetectCategoriesButton(
                    onClick = onAutoDetectCategories,
                    enabled = uiState.isConfigNotBlank() && !uiState.isDetectingSupportedCategories,
                    isDetecting = uiState.isDetectingSupportedCategories,
                )
            }
        }
    }
}

private fun TorznabConnectionCheckResult.displayName(resources: Resources): String {
    if (this is TorznabConnectionCheckResult.ApplicationError) {
        return resources.getString(
            R.string.torznab_conn_check_result_app_error,
            this.errorCode,
        )
    }

    if (this is TorznabConnectionCheckResult.UnexpectedResponse) {
        return resources.getString(
            R.string.torznab_conn_check_result_unexpected_response,
            this.errorCode,
        )
    }

    val otherResId = when (this) {
        TorznabConnectionCheckResult.ConnectionFailed -> {
            R.string.torznab_conn_check_result_conn_failed
        }

        TorznabConnectionCheckResult.InvalidApiKey -> {
            R.string.torznab_conn_check_result_invalid_api_key
        }

        TorznabConnectionCheckResult.ConnectionEstablished -> {
            R.string.torznab_conn_check_result_conn_established
        }

        TorznabConnectionCheckResult.UnexpectedError -> {
            R.string.torznab_conn_check_result_unexpected_error
        }
    }

    return resources.getString(otherResId)
}