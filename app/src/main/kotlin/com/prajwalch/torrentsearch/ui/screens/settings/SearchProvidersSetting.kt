package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProviderUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun SearchProvidersSetting(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.searchSettingsUiState.collectAsStateWithLifecycle()
    val searchProvidersUiState by remember { derivedStateOf { settings.searchProviders } }

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchProvidersSettingTopBar(onNavigateBack = onNavigateBack)
        }
    ) { innerPadding ->
        SearchProviderList(
            modifier = Modifier.consumeWindowInsets(innerPadding),
            contentPadding = innerPadding,
            searchProviders = searchProvidersUiState,
            onProviderCheckedChange = viewModel::enableSearchProvider,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchProvidersSettingTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.setting_search_providers)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.button_go_to_settings_screen),
                )
            }
        }
    )
}

@Composable
private fun SearchProviderList(
    searchProviders: List<SearchProviderUiState>,
    onProviderCheckedChange: (SearchProviderId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        items(items = searchProviders, key = { it.id }) { searchProvider ->
            SearchProviderListItem(
                checked = searchProvider.enabled,
                onCheckedChange = { onProviderCheckedChange(searchProvider.id, it) },
                name = searchProvider.name
            )
        }
    }
}

@Composable
private fun SearchProviderListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = Modifier
            .clickable(role = Role.Checkbox, onClick = { onCheckedChange(!checked) })
            .then(modifier),
        headlineContent = { Text(text = name) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}