package com.prajwalch.torrentsearch.ui.torrentactions.component

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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ActionListItem(
    onClick: () -> Unit,
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ListItemColors = ListItemDefaults.colors(enabled),
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick, enabled = enabled),
        leadingContent = {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = icon,
                contentDescription = label,
            )
        },
        headlineContent = {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        colors = colors,
    )
}

@Composable
private fun ListItemDefaults.colors(enabled: Boolean): ListItemColors {
    return if (enabled) {
        colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    } else {
        with(colors()) {
            copy(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                headlineColor = disabledHeadlineColor,
                leadingIconColor = disabledLeadingIconColor,
            )
        }
    }
}