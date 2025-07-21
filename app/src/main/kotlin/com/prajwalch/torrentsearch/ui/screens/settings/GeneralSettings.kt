package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun GeneralSettings(modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.generalSettingsUiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_general)
        SettingsItem(
            onClick = { viewModel.updateEnableNSFWMode(!settings.enableNSFWMode) },
            leadingIconId = R.drawable.ic_18_up_rating,
            headlineId = R.string.setting_enable_nsfw_mode,
            trailingContent = {
                Switch(
                    checked = settings.enableNSFWMode,
                    onCheckedChange = { viewModel.updateEnableNSFWMode(it) },
                )
            },
        )
    }
}