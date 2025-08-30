package com.prajwalch.torrentsearch.ui.screens.settings

import android.os.Build

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.DarkTheme
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.DialogListItem
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun AppearanceSettings(modifier: Modifier = Modifier) {
    val viewModel = activityScopedViewModel<SettingsViewModel>()
    val settings by viewModel.appearanceSettingsUiState.collectAsStateWithLifecycle()

    var showDarkThemeDialog by remember(settings) { mutableStateOf(false) }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val showPureBlackSetting = remember(settings.darkTheme, isSystemInDarkTheme) {
        when (settings.darkTheme) {
            DarkTheme.On -> true
            DarkTheme.Off -> false
            DarkTheme.FollowSystem -> isSystemInDarkTheme
        }
    }

    if (showDarkThemeDialog) {
        DarkThemeOptionsDialog(
            onDismissRequest = { showDarkThemeDialog = false },
            selectedOption = settings.darkTheme,
            onOptionSelect = { viewModel.changeDarkTheme(it) },
        )
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

        SettingsItem(
            onClick = { showDarkThemeDialog = true },
            leadingIconId = R.drawable.ic_dark_mode,
            headlineId = R.string.setting_dark_theme,
            supportingContent = settings.darkTheme.toString(),
        )

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

@Composable
private fun DarkThemeOptionsDialog(
    onDismissRequest: () -> Unit,
    selectedOption: DarkTheme,
    onOptionSelect: (DarkTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        titleId = R.string.setting_dark_theme,
    ) {
        LazyColumn {
            items(
                items = DarkTheme.entries,
                contentType = { it }
            ) { darkThemeOpt ->
                DarkThemeOptionItem(
                    selected = darkThemeOpt == selectedOption,
                    onClick = { onOptionSelect(darkThemeOpt) },
                    name = darkThemeOpt.toString(),
                )
            }
        }
    }
}

@Composable
private fun DarkThemeOptionItem(
    selected: Boolean,
    onClick: () -> Unit,
    name: String,
    modifier: Modifier = Modifier,
) {
    DialogListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        headlineContent = { Text(text = name) },
    )
}