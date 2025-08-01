package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import kotlinx.coroutines.launch

@Composable
fun SearchHistorySettings(snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.searchHistorySettingsUiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_search_history)
        SettingsItem(
            onClick = { viewModel.saveSearchHistory(!settings.saveSearchHistory) },
            leadingIconId = R.drawable.ic_search_activity,
            headlineId = R.string.setting_save_search_history,
            trailingContent = {
                Switch(
                    checked = settings.saveSearchHistory,
                    onCheckedChange = { viewModel.saveSearchHistory(it) },
                )
            },
        )
        SettingsItem(
            onClick = { viewModel.showSearchHistory(!settings.showSearchHistory) },
            leadingIconId = R.drawable.ic_history_toggle_off,
            headlineId = R.string.setting_show_search_history,
            supportingContent = stringResource(R.string.setting_show_search_history_desc),
            trailingContent = {
                Switch(
                    checked = settings.showSearchHistory,
                    onCheckedChange = { viewModel.showSearchHistory(it) },
                )
            },
        )

        val searchHistoryClearedHint = stringResource(R.string.hint_search_history_cleared)
        SettingsItem(
            onClick = {
                viewModel.clearSearchHistory()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(searchHistoryClearedHint)
                }
            },
            leadingIconId = R.drawable.ic_delete_history,
            headlineId = R.string.setting_clear_search_history,
        )
    }
}