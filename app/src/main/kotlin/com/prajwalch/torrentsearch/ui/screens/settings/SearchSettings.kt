package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.Settings
import com.prajwalch.torrentsearch.ui.components.SettingsListItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun SearchSettings(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
) {
    SettingsSectionTitle(stringResource(R.string.settings_section_search))
    SettingsListItem(
        leadingIconId = R.drawable.ic_18_up_rating,
        headline = stringResource(R.string.setting_enable_nsfw_search),
        trailingContent = {
            Switch(
                checked = settings.enableNSFWSearch,
                onCheckedChange = {
                    onSettingsChange(settings.copy(enableNSFWSearch = it))
                }
            )
        },
        onClick = {
            onSettingsChange(
                settings.copy(enableNSFWSearch = !settings.enableNSFWSearch)
            )
        }
    )
    SettingsListItem(
        leadingIconId = R.drawable.ic_graph,
        headline = stringResource(R.string.setting_search_providers),
        onClick = {}
    )
}