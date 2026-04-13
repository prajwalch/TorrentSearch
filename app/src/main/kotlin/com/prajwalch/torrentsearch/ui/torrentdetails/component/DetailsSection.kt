package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun DetailsSection(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleMedium,
        ) {
            title()
        }
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) {
            content()
        }
    }
}