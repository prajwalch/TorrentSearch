package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProviderUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun SearchSettings(viewModel: SettingsViewModel) {
    val settings by viewModel.searchSettings.collectAsState()
    var showListDialog by remember { mutableStateOf(false) }

    if (showListDialog) {
        SearchProvidersListDialog(
            searchProviders = settings.searchProviders1,
            onDismissRequest = { showListDialog = false },
            onCheckedChange = { providerId, enable ->
                viewModel.enableSearchProvider(
                    providerId = providerId,
                    enable = enable
                )
            }
        )
    }

    SettingsSectionTitle(stringResource(R.string.settings_section_search))
    SettingsItem(
        leadingIconId = R.drawable.ic_18_up_rating,
        headline = stringResource(R.string.setting_enable_nsfw_search),
        trailingContent = {
            Switch(
                checked = settings.enableNSFWSearch,
                onCheckedChange = { viewModel.updateEnableNSFWSearch(it) }
            )
        },
        onClick = { viewModel.updateEnableNSFWSearch(!settings.enableNSFWSearch) }
    )
    SettingsItem(
        leadingIconId = R.drawable.ic_visibility_off,
        headline = stringResource(R.string.setting_hide_results_with_zero_seeders),
        trailingContent = {
            Switch(
                checked = settings.hideResultsWithZeroSeeders,
                onCheckedChange = { viewModel.updateHideResultsWithZeroSeeders(it) }
            )
        },
        onClick = {
            viewModel.updateHideResultsWithZeroSeeders(!settings.hideResultsWithZeroSeeders)
        }
    )
    SettingsItem(
        leadingIconId = R.drawable.ic_graph,
        headline = stringResource(R.string.setting_search_providers),
        supportingContent = stringResource(
            R.string.x_of_x_enabled,
            settings.enabledSearchProviders,
            settings.totalSearchProviders
        ),
        onClick = {
            showListDialog = true
        }
    )
}

@Composable
private fun SearchProvidersListDialog(
    searchProviders: List<SearchProviderUiState>,
    onDismissRequest: () -> Unit,
    onCheckedChange: (SearchProviderId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDialog(
        modifier = modifier,
        title = R.string.setting_search_providers,
        onDismissRequest = onDismissRequest,
        content = {
            SearchProvidersList(
                searchProviders = searchProviders,
                onCheckedChange = onCheckedChange,
            )
        }
    )
}

@Composable
private fun SearchProvidersList(
    searchProviders: List<SearchProviderUiState>,
    onCheckedChange: (SearchProviderId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {

    LazyColumn(modifier = modifier) {
        items(
            items = searchProviders,
            key = { it.id },
        ) { searchProvider ->
            SearchProvidersListItem(
                enabled = searchProvider.enabled,
                name = searchProvider.name,
                onCheckedChange = { onCheckedChange(searchProvider.id, it) }
            )
        }
    }
}

@Composable
private fun SearchProvidersListItem(
    enabled: Boolean,
    name: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ListItemDefaults.colors(containerColor = Color.Unspecified)

    ListItem(
        modifier = modifier.clickable(onClick = { onCheckedChange(!enabled) }),
        leadingContent = {
            Checkbox(
                checked = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
        headlineContent = { Text(text = name) },
        colors = colors,
    )
}