package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ScreenshotImage(url: String, modifier: Modifier = Modifier) {
    NetworkImage(
        modifier = modifier
            .height(180.dp)
            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.0.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            ),
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}