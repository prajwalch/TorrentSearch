package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R

@Composable
fun TorrentsCount(count: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.browse_torrents_count_format, count),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}