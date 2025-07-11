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
import com.prajwalch.torrentsearch.providers.ProviderId
import com.prajwalch.torrentsearch.providers.SearchProviders
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun SearchSettings(viewModel: SettingsViewModel) {
    val settings by viewModel.searchSettings.collectAsState()

    val allProviders = remember { SearchProviders.namesWithId() }
    var showListDialog by remember { mutableStateOf(false) }

    if (showListDialog) {
        SearchProvidersListDialog(
            allProviders = allProviders,
            enabledProviders = settings.searchProviders,
            onDismissRequest = { showListDialog = false },
            onProvidersChange = { viewModel.updateSearchProviders(it) }
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
        leadingIconId = R.drawable.ic_graph,
        headline = stringResource(R.string.setting_search_providers),
        supportingContent = stringResource(
            R.string.x_of_x_enabled,
            settings.searchProviders.size,
            allProviders.size
        ),
        onClick = {
            showListDialog = true
        }
    )
}

@Composable
private fun SearchProvidersListDialog(
    allProviders: List<Pair<ProviderId, String>>,
    enabledProviders: Set<ProviderId>,
    onDismissRequest: () -> Unit,
    onProvidersChange: (Set<ProviderId>) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDialog(
        modifier = modifier,
        title = R.string.setting_search_providers,
        onDismissRequest = onDismissRequest,
        content = {
            SearchProvidersList(
                allProviders = allProviders,
                enabledProviders = enabledProviders,
                onProviderClick = { (id, enable) ->
                    val newEnabledProviders = if (enable) {
                        enabledProviders + id
                    } else {
                        enabledProviders - id
                    }
                    onProvidersChange(newEnabledProviders)
                },
            )
        }
    )
}

@Composable
private fun SearchProvidersList(
    allProviders: List<Pair<ProviderId, String>>,
    enabledProviders: Set<ProviderId>,
    onProviderClick: (Pair<ProviderId, Boolean>) -> Unit,
    modifier: Modifier = Modifier,
) {

    LazyColumn(modifier = modifier) {
        items(
            items = allProviders,
            key = { (id, _) -> id },
        ) { (id, name) ->
            SearchProvidersListItem(
                enabled = enabledProviders.contains(id),
                name = name,
                onClick = { onProviderClick(Pair(id, it)) }
            )
        }
    }
}

@Composable
private fun SearchProvidersListItem(
    enabled: Boolean,
    name: String,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ListItemDefaults.colors(containerColor = Color.Unspecified)

    ListItem(
        modifier = modifier.clickable(onClick = { onClick(!enabled) }),
        leadingContent = {
            Checkbox(
                checked = enabled,
                onCheckedChange = onClick,
            )
        },
        headlineContent = { Text(text = name) },
        colors = colors,
    )
}