package com.prajwalch.torrentsearch.ui.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SettingsItemCard(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        ) {
            SettingsItemCardHeader(
                title = title,
                subtitle = subtitle,
                leadingIcon = leadingIcon,
            )

            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItemCardHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            leadingIcon?.invoke()
        }

        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                LocalTextStyle provides MaterialTheme.typography.bodyLarge,
                content = title,
            )

            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            ) {
                subtitle?.invoke()
            }
        }
    }
}

@Composable
fun SettingsItemCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: Painter? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsItemCard(
        modifier = modifier,
        title = { Text(title) },
        subtitle = subtitle?.let {
            {
                Text(it)
            }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(painter = it, contentDescription = null)
            }
        },
        content = content,
    )
}

@Preview
@Composable
private fun SettingsItemCardPreview() {
    TorrentSearchTheme {
        SettingsItemCard(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_storage),
                    contentDescription = null,
                )
            },
            title = { Text("DNS over HTTPS (DoH)") },
            subtitle = { Text("Determines how to connect with website securely") },
        ) {
            Text("Card content")
        }
    }
}

@Preview
@Composable
private fun SettingsItemCardWrapperPreview() {
    TorrentSearchTheme {
        SettingsItemCard(
            title = "DNS over HTTPS (DoH)",
            subtitle = "Determines how to connect with website securely",
            leadingIcon = painterResource(R.drawable.ic_storage),
        ) {
            Text("Card content")
        }
    }
}