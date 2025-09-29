package com.prajwalch.torrentsearch.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun AdvancedSettings(modifier: Modifier = Modifier) {
    val viewModel = activityScopedViewModel<SettingsViewModel>()
    val uiState by viewModel.advanceSettingsUiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val packageManager = context.packageManager

    Column(modifier = modifier) {
        SettingsSectionTitle(R.string.settings_section_advanced)

        SettingsItem(
            onClick = {
                viewModel.enableShareIntegration(
                    enable = !uiState.enableShareIntegration,
                    packageManager = packageManager,
                )
            },
            leadingIconId = R.drawable.ic_share,
            headlineId = R.string.setting_enable_share_integration,
            supportingContent = stringResource(
                R.string.setting_enable_share_integration_supporting_text,
            ),
            trailingContent = {
                Switch(
                    checked = uiState.enableShareIntegration,
                    onCheckedChange = {
                        viewModel.enableShareIntegration(
                            enable = it,
                            packageManager = packageManager,
                        )
                    },
                )
            },
        )
        SettingsItem(
            onClick = {
                viewModel.enableQuickSearch(
                    enable = !uiState.enableQuickSearch,
                    packageManager = packageManager,
                )
            },
            leadingIconId = R.drawable.ic_search,
            headlineId = R.string.setting_enable_quick_search,
            supportingContent = stringResource(
                R.string.setting_enable_quick_search_supporting_text,
            ),
            trailingContent = {
                Switch(
                    checked = uiState.enableQuickSearch,
                    onCheckedChange = {
                        viewModel.enableQuickSearch(
                            enable = it,
                            packageManager = packageManager,
                        )
                    },
                )
            },
        )
    }
}