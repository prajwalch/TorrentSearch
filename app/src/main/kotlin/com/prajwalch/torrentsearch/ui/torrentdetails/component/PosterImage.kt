package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object PosterImageDefaults {
    val Height: Dp = 240.dp

    val AspectRatio: Float = 2f / 3f

    val Shape: Shape
        @Composable get() = MaterialTheme.shapes.medium

    val BorderStroke: BorderStroke
        @Composable get() = BorderStroke(
            width = 2.0.dp,
            color = MaterialTheme.colorScheme.outline,
        )
}

@Composable
fun PosterImage(
    url: String,
    modifier: Modifier = Modifier,
    height: Dp = PosterImageDefaults.Height,
    aspectRatio: Float = PosterImageDefaults.AspectRatio,
    shape: Shape = PosterImageDefaults.Shape,
) {
    NetworkImage(
        modifier = modifier
            .height(height)
            .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
            .clip(shape)
            .border(border = PosterImageDefaults.BorderStroke, shape = shape),
        model = url,
        contentDescription = null,
    )
}