package com.prajwalch.torrentsearch.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun GeneralSettings(modifier: Modifier = Modifier) {
    val viewModel = activityScopedViewModel<SettingsViewModel>()
    val settings by viewModel.generalSettingsUiState.collectAsStateWithLifecycle()


    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_general)

        val context = LocalContext.current
        SettingsItem(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.openAppLocaleSettings()
                }
            },
            leadingIconId = R.drawable.ic_language,
            headlineId = R.string.setting_language,
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Context.openAppLocaleSettings() {
    val appUri = Uri.fromParts("package", this.packageName, null)
    val localeSettingsIntent = Intent().apply {
        action = Settings.ACTION_APP_LOCALE_SETTINGS
        data = appUri
    }

    this.startActivity(localeSettingsIntent)
}