package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.DarkTheme
import com.prajwalch.torrentsearch.data.Settings
import com.prajwalch.torrentsearch.ui.components.SettingsListItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun AppearanceSettings(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onOpenOptionMenu: (SettingsOptionMenuEvent) -> Unit,
) {
    SettingsSectionTitle(title = stringResource(R.string.settings_section_appearance))
    SettingsListItem(
        leadingIconId = R.drawable.ic_palette,
        headline = stringResource(R.string.setting_enable_dynamic_theme),
        trailingContent = {
            Switch(checked = settings.enableDynamicTheme, onCheckedChange = {
                onSettingsChange(settings.copy(enableDynamicTheme = it))
            })
        },
        onClick = {
            onSettingsChange(
                settings.copy(enableDynamicTheme = !settings.enableDynamicTheme)
            )
        }
    )
    SettingsListItem(
        leadingIconId = R.drawable.ic_dark_mode,
        headline = stringResource(R.string.setting_dark_theme),
        supportingContent = settings.darkTheme.toString(),
        onClick = {
            onOpenOptionMenu(
                SettingsOptionMenuEvent(
                    title = R.string.setting_dark_theme,
                    selectedOption = settings.darkTheme.ordinal,
                    options = DarkTheme.entries.map { it.toString() },
                    onSelected = {
                        onSettingsChange(
                            settings.copy(darkTheme = DarkTheme.Companion.fromInt(it))
                        )
                    }
                )
            )
        }
    )
}