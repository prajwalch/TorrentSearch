package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations

@Composable
fun CoverImage(
    url: String,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
) {
    val gradientOverlay = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface,
        ),
    )

    AsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawWithContent {
                drawContent()
                drawRect(gradientOverlay)
            },
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .transformations(BlurTransformation())
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
    )
}