package com.prajwalch.torrentsearch.ui.screens.settings

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
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun GeneralSettings(onNavigateToDefaultCategory: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.generalSettingsUiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_general)
        SettingsItem(
            onClick = onNavigateToDefaultCategory,
            leadingIconId = R.drawable.ic_category_search,
            headlineId = R.string.setting_default_category,
            supportingContent = settings.defaultCategory.name,
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = stringResource(R.string.button_go_to_category_list_screen),
                )
            }
        )
        SettingsItem(
            onClick = { viewModel.enableNSFWMode(!settings.enableNSFWMode) },
            leadingIconId = R.drawable.ic_18_up_rating,
            headlineId = R.string.setting_enable_nsfw_mode,
            trailingContent = {
                Switch(
                    checked = settings.enableNSFWMode,
                    onCheckedChange = { viewModel.enableNSFWMode(it) },
                )
            },
        )
    }
}