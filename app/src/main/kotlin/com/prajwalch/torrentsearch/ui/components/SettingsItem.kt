package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun SettingsItem(
    @DrawableRes
    leadingIconId: Int,
    headline: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
        leadingContent = {
            Icon(
                painter = painterResource(leadingIconId),
                contentDescription = headline,
            )
        },
        headlineContent = { Text(text = headline) },
        supportingContent = supportingContent?.let { { Text(text = it) } },
        trailingContent = trailingContent,
    )
}