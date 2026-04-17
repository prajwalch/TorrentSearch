package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage

@Composable
fun HeroBackgroundImage(url: String, modifier: Modifier = Modifier) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    AsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .drawWithContent {
                drawContent()
                drawRect(color = Color.Black.copy(alpha = 0.3f))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, surfaceColor)))
            },
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}