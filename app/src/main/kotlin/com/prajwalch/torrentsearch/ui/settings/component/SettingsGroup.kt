package com.prajwalch.torrentsearch.ui.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}