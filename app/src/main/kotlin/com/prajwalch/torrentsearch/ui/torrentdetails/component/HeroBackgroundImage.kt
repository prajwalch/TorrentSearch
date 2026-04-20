package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations

@Composable
fun HeroBackgroundImage(
    url: String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    revealed: Boolean = true,
) {
    // A semi-transparent black background which sits between the image
    // and the gradient overlay.
    val scrimOverlay = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    // A top layer gradient which smoothly fades out the image at the bottom.
    val gradientOverlay = Brush.verticalGradient(
        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
    )

    // Apply effects (scrim, gradient and blur) only after image loads successfully.
    var applyEffects by rememberSaveable { mutableStateOf(false) }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .apply { if (applyEffects && !revealed) transformations(BlurTransformation()) }
        .build()

    AsyncImage(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .overlayEffects(
                scrim = scrimOverlay,
                gradient = gradientOverlay,
                enabled = applyEffects,
            ),
        model = imageRequest,
        contentDescription = null,
        onSuccess = { applyEffects = true },
        contentScale = ContentScale.Crop,
    )
}

private fun Modifier.overlayEffects(
    scrim: Color,
    gradient: Brush,
    enabled: Boolean,
): Modifier {
    val effectsModifier = if (!enabled) {
        Modifier
    } else {
        Modifier.drawWithContent {
            drawContent()
            drawRect(color = scrim)
            drawRect(brush = gradient)
        }
    }

    return this.then(effectsModifier)
}