package com.prajwalch.torrentsearch.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.BuildConfig
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun About(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    val repoUrl = "https://github.com/prajwalch/TorrentSearch"
    val currentReleaseUrl = "$repoUrl/releases/tag/v${BuildConfig.VERSION_NAME}"

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_about)
        SettingsItem(
            onClick = { uriHandler.openUri(uri = currentReleaseUrl) },
            leadingIconId = R.drawable.ic_info,
            headlineId = R.string.setting_version,
            supportingContent = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                )
            }
        )
        SettingsItem(
            onClick = { uriHandler.openUri(uri = repoUrl) },
            leadingIconId = R.drawable.ic_code,
            headlineId = R.string.setting_source_code,
            supportingContent = repoUrl.removePrefix("https://"),
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                )
            }
        )
    }
}