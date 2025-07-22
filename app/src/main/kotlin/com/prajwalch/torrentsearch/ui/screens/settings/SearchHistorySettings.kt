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
            onClick = { viewModel.pauseSearchHistory(!settings.pauseSearchHistory) },
            leadingIconId = R.drawable.ic_history_off,
            headlineId = R.string.setting_pause_search_history,
            trailingContent = {
                Switch(
                    checked = settings.pauseSearchHistory,
                    onCheckedChange = { viewModel.pauseSearchHistory(it) },
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