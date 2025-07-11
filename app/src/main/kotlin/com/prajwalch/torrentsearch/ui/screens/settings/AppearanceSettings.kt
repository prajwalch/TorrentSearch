package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.DarkTheme
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsOptionMenu
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel


data class SettingsOptionMenuEvent(
    @param:StringRes
    val title: Int,
    val selectedOption: Int,
    val options: List<String>,
    val onSelected: (Int) -> Unit,
)

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    val settings by viewModel.appearanceSettings.collectAsState()
    var optionMenuEvent by remember(settings) { mutableStateOf<SettingsOptionMenuEvent?>(null) }

    optionMenuEvent?.let { event ->
        SettingsOptionMenu(
            title = event.title,
            selectedOption = event.selectedOption,
            options = event.options,
            onSelected = event.onSelected,
            onDismissRequest = { optionMenuEvent = null },
        )
    }

    SettingsSectionTitle(title = stringResource(R.string.settings_section_appearance))
    SettingsItem(
        leadingIconId = R.drawable.ic_palette,
        headline = stringResource(R.string.setting_enable_dynamic_theme),
        trailingContent = {
            Switch(checked = settings.enableDynamicTheme, onCheckedChange = {
                viewModel.updateEnableDynamicTheme(it)
            })
        },
        onClick = {
            viewModel.updateEnableDynamicTheme(!settings.enableDynamicTheme)
        }
    )
    SettingsItem(
        leadingIconId = R.drawable.ic_dark_mode,
        headline = stringResource(R.string.setting_dark_theme),
        supportingContent = settings.darkTheme.toString(),
        onClick = {
            optionMenuEvent = SettingsOptionMenuEvent(
                title = R.string.setting_dark_theme,
                selectedOption = settings.darkTheme.ordinal,
                options = DarkTheme.entries.map { it.toString() },
                onSelected = { viewModel.updateDarkTheme(DarkTheme.fromInt(it)) }
            )
        }
    )
}