package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage

@Composable
fun MediaPoster(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        modifier = Modifier
            .width(120.dp)
            .clip(MaterialTheme.shapes.medium)
            .aspectRatio(2f / 3f)
            .then(modifier),
        model = url,
        contentDescription = null,
    )
}