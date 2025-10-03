package com.prajwalch.torrentsearch.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun SearchHistorySettings(onNavigateToSearchHistory: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = activityScopedViewModel<SettingsViewModel>()
    val settings by viewModel.searchHistorySettingsUiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_search_history)
        SettingsItem(
            onClick = { viewModel.enableSaveSearchHistory(!settings.saveSearchHistory) },
            leadingIconId = R.drawable.ic_search_activity,
            headlineId = R.string.setting_save_search_history,
            trailingContent = {
                Switch(
                    checked = settings.saveSearchHistory,
                    onCheckedChange = { viewModel.enableSaveSearchHistory(it) },
                )
            },
        )
        SettingsItem(
            onClick = { viewModel.enableShowSearchHistory(!settings.showSearchHistory) },
            leadingIconId = R.drawable.ic_history_toggle_off,
            headlineId = R.string.setting_show_search_history,
            supportingContent = stringResource(R.string.setting_show_search_history_desc),
            trailingContent = {
                Switch(
                    checked = settings.showSearchHistory,
                    onCheckedChange = { viewModel.enableShowSearchHistory(it) },
                )
            },
        )
        SettingsItem(
            onClick = onNavigateToSearchHistory,
            leadingIconId = R.drawable.ic_manage_history,
            headlineId = R.string.setting_manage_search_history,
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = stringResource(R.string.desc_go_to_search_history_screen),
                )
            },
        )
    }
}