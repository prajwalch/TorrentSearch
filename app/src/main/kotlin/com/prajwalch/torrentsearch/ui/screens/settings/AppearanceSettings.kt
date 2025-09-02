package com.prajwalch.torrentsearch.ui.screens.settings

import android.os.Build

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.DarkTheme
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun AppearanceSettings(modifier: Modifier = Modifier) {
    val viewModel = activityScopedViewModel<SettingsViewModel>()
    val settings by viewModel.appearanceSettingsUiState.collectAsStateWithLifecycle()

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val showPureBlackSetting = remember(settings.darkTheme, isSystemInDarkTheme) {
        when (settings.darkTheme) {
            DarkTheme.On -> true
            DarkTheme.Off -> false
            DarkTheme.FollowSystem -> isSystemInDarkTheme
        }
    }

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_appearance)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsItem(
                onClick = { viewModel.enableDynamicTheme(!settings.enableDynamicTheme) },
                leadingIconId = R.drawable.ic_palette,
                headlineId = R.string.setting_enable_dynamic_theme,
                trailingContent = {
                    Switch(
                        checked = settings.enableDynamicTheme,
                        onCheckedChange = { viewModel.enableDynamicTheme(it) },
                    )
                },
            )
        }

        Box {
            var menuExpanded by remember(settings.darkTheme) { mutableStateOf(false) }

            SettingsItem(
                onClick = { menuExpanded = true },
                leadingIconId = R.drawable.ic_dark_mode,
                headlineId = R.string.setting_dark_theme,
                supportingContent = settings.darkTheme.toString(),
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset = DpOffset(x = 16.dp, y = 0.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                DarkTheme.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(text = it.toString()) },
                        onClick = { viewModel.changeDarkTheme(it) },
                        leadingIcon = {
                            RadioButton(
                                selected = it == settings.darkTheme,
                                onClick = { viewModel.changeDarkTheme(it) },
                            )
                        }
                    )
                }
            }
        }

        AnimatedVisibility(visible = showPureBlackSetting) {
            SettingsItem(
                onClick = { viewModel.enablePureBlackTheme(!settings.pureBlack) },
                leadingIconId = R.drawable.ic_contrast,
                headlineId = R.string.setting_pure_black,
                trailingContent = {
                    Switch(
                        checked = settings.pureBlack,
                        onCheckedChange = { viewModel.enablePureBlackTheme(it) },
                    )
                },
            )
        }
    }
}