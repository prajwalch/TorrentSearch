package com.prajwalch.torrentsearch.ui.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun SettingsListItem(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors().copy(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    shape: Shape = MaterialTheme.shapes.large,
) {
    ListItem(
        modifier = Modifier
            .clip(shape)
            .then(modifier),
        headlineContent = title,
        supportingContent = subtitle,
        leadingContent = leadingIcon,
        trailingContent = trailingContent,
        colors = colors,
    )
}

@Composable
fun SettingsListItem(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: Painter? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SettingsListItem(
        modifier = modifier.clickable(onClick = onClick),
        title = { Text(title) },
        subtitle = subtitle?.let {
            {
                Text(it)
            }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = it,
                    contentDescription = null,
                )
            }
        },
        trailingContent = trailingContent,
    )
}