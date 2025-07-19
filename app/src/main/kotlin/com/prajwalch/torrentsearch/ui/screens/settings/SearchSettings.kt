package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.MaxNumResults
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProviderUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettings(viewModel: SettingsViewModel) {
    val settings by viewModel.searchSettingsUiState.collectAsState()

    var showListDialog by remember { mutableStateOf(false) }
    var showMaxNumResultsDialog by remember { mutableStateOf(false) }

    if (showListDialog) {
        SettingsDialog(
            title = R.string.setting_search_providers,
            onDismissRequest = { showListDialog = false },
        ) {
            SearchProvidersList(
                searchProviders = settings.searchProviders,
                onProviderCheckedChange = viewModel::enableSearchProvider,
            )
        }
    }

    if (showMaxNumResultsDialog) {
        MaxNumResultsDialog(
            onDismissRequest = { showMaxNumResultsDialog = false },
            num = if (settings.maxNumResults.isUnlimited()) null else settings.maxNumResults.n,
            onNumChange = { viewModel.updateMaxNumResults(MaxNumResults(n = it)) },
            onUnlimitedClick = {
                showMaxNumResultsDialog = false
                viewModel.updateMaxNumResults(MaxNumResults.Unlimited)
            },
        )
    }

    SettingsSectionTitle(title = stringResource(R.string.settings_section_search))
    SettingsItem(
        leadingIconId = R.drawable.ic_18_up_rating,
        headline = stringResource(R.string.setting_enable_nsfw_search),
        onClick = { viewModel.updateEnableNSFWSearch(!settings.enableNSFWSearch) },
        trailingContent = {
            Switch(
                checked = settings.enableNSFWSearch,
                onCheckedChange = { viewModel.updateEnableNSFWSearch(it) },
            )
        },
    )
    SettingsItem(
        leadingIconId = R.drawable.ic_visibility_off,
        headline = stringResource(R.string.setting_hide_results_with_zero_seeders),
        onClick = {
            viewModel.updateHideResultsWithZeroSeeders(!settings.hideResultsWithZeroSeeders)
        },
        trailingContent = {
            Switch(
                checked = settings.hideResultsWithZeroSeeders,
                onCheckedChange = { viewModel.updateHideResultsWithZeroSeeders(it) },
            )
        },
    )
    SettingsItem(
        leadingIconId = R.drawable.ic_graph,
        headline = stringResource(R.string.setting_search_providers),
        onClick = { showListDialog = true },
        supportingContent = stringResource(
            R.string.x_of_x_enabled,
            settings.enabledSearchProviders,
            settings.totalSearchProviders
        ),
    )
    SettingsItem(
        leadingIconId = R.drawable.ic_format_list_numbered,
        headline = stringResource(R.string.setting_max_num_results),
        onClick = { showMaxNumResultsDialog = true },
        supportingContent = if (settings.maxNumResults.isUnlimited()) {
            stringResource(R.string.unlimited)
        } else {
            settings.maxNumResults.n.toString()
        },
    )
}

@Composable
private fun SearchProvidersList(
    searchProviders: List<SearchProviderUiState>,
    onProviderCheckedChange: (SearchProviderId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            items = searchProviders,
            key = { it.id },
        ) { searchProvider ->
            SearchProvidersListItem(
                checked = searchProvider.enabled,
                onCheckedChange = { onProviderCheckedChange(searchProvider.id, it) },
                name = searchProvider.name,
            )
        }
    }
}

@Composable
private fun SearchProvidersListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = Color.Unspecified
    ),
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = { onCheckedChange(!checked) })
            .then(modifier),
        leadingContent = {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        },
        headlineContent = { Text(text = name) },
        colors = colors,
    )
}

@Composable
private fun MaxNumResultsDialog(
    onDismissRequest: () -> Unit,
    num: Int?,
    onNumChange: (Int) -> Unit,
    onUnlimitedClick: () -> Unit,
    modifier: Modifier = Modifier,
    sliderRange: ClosedFloatingPointRange<Float> = 10f..100f,
    incrementBy: Int = 5,
) {
    var sliderValue by remember(num) {
        mutableFloatStateOf(num?.toFloat() ?: sliderRange.start)
    }

    SettingsDialog(
        title = R.string.setting_max_num_results,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDismissRequest()
                onNumChange(sliderValue.toInt())
            }) {
                Text(text = stringResource(R.string.button_done))
            }
        }
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.max_n_results, sliderValue.toInt()),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = (newValue / incrementBy) * incrementBy
                },
                valueRange = sliderRange,
                steps = ((sliderRange.endInclusive - sliderRange.start) / incrementBy).toInt() - 1,
            )
            OutlinedButton(onClick = onUnlimitedClick) {
                Text(text = stringResource(R.string.unlimited))
            }
        }
    }
}