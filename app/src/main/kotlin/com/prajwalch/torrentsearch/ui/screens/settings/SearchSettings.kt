package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.MaxNumResults
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun SearchSettings(
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToDefaultSortOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.searchSettingsUiState.collectAsStateWithLifecycle()

    var showMaxNumResultsDialog by remember { mutableStateOf(false) }

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

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_search)
        SettingsItem(
            onClick = onNavigateToSearchProviders,
            leadingIconId = R.drawable.ic_travel_explore,
            headlineId = R.string.setting_search_providers,
            supportingContent = stringResource(
                R.string.x_of_x_enabled,
                settings.enabledSearchProviders,
                settings.totalSearchProviders
            ),
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = stringResource(
                        R.string.button_go_to_search_provider_list_screen
                    ),
                )
            },
        )

        val defaultSortCriteria = settings.defaultSortOptions.sortCriteria
        val defaultSortOrder = settings.defaultSortOptions.sortOrder
        SettingsItem(
            onClick = onNavigateToDefaultSortOptions,
            leadingIconId = R.drawable.ic_sort,
            headlineId = R.string.setting_default_sort_options,
            supportingContent = "$defaultSortCriteria / $defaultSortOrder",
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = stringResource(
                        R.string.button_go_to_default_sort_options_screen,
                    ),
                )
            },
        )
        SettingsItem(
            onClick = {
                viewModel.updateHideResultsWithZeroSeeders(!settings.hideResultsWithZeroSeeders)
            },
            leadingIconId = R.drawable.ic_visibility_off,
            headlineId = R.string.setting_hide_results_with_zero_seeders,
            trailingContent = {
                Switch(
                    checked = settings.hideResultsWithZeroSeeders,
                    onCheckedChange = { viewModel.updateHideResultsWithZeroSeeders(it) },
                )
            },
        )
        SettingsItem(
            onClick = { showMaxNumResultsDialog = true },
            leadingIconId = R.drawable.ic_format_list_numbered,
            headlineId = R.string.setting_max_num_results,
            supportingContent = if (settings.maxNumResults.isUnlimited()) {
                stringResource(R.string.unlimited)
            } else {
                settings.maxNumResults.n.toString()
            },
        )
    }
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
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        titleId = R.string.setting_max_num_results,
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
                onValueChange = { sliderValue = (it / incrementBy) * incrementBy },
                valueRange = sliderRange,
                steps = ((sliderRange.endInclusive - sliderRange.start) / incrementBy).toInt() - 1,
            )
            OutlinedButton(onClick = onUnlimitedClick) {
                Text(text = stringResource(R.string.unlimited))
            }
        }
    }
}