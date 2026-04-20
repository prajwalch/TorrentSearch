package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations

import com.prajwalch.torrentsearch.R

@Composable
fun MediaPoster(
    url: String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    aspectRatio: Float = 2f / 3f,
    shape: Shape = MaterialTheme.shapes.medium,
    background: Color = MaterialTheme.colorScheme.surfaceContainer,
    onSuccess: (() -> Unit)? = null,
    enableBlur: Boolean = false,
) {
    var imageLoaded by rememberSaveable(url) { mutableStateOf(false) }

    val contentScale = if (imageLoaded) ContentScale.Crop else ContentScale.None
    val colorFilter = if (imageLoaded) {
        null
    } else {
        ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
    }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .apply { if (imageLoaded && enableBlur) transformations(BlurTransformation()) }
        .build()

    AsyncImage(
        modifier = Modifier
            .height(height)
            .aspectRatio(ratio = aspectRatio, matchHeightConstraintsFirst = true)
            .clip(shape)
            .background(color = background, shape = shape)
            .then(modifier),
        model = imageRequest,
        contentDescription = null,
        placeholder = painterResource(R.drawable.ic_downloading),
        error = painterResource(R.drawable.ic_error),
        fallback = painterResource(R.drawable.ic_image),
        onSuccess = {
            imageLoaded = true
            onSuccess?.let { it() }
        },
        contentScale = contentScale,
        colorFilter = colorFilter,
    )
}