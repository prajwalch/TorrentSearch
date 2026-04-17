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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

import com.prajwalch.torrentsearch.R

@Composable
fun MediaPoster(url: String, modifier: Modifier = Modifier) {
    var isSuccess by rememberSaveable(url) { mutableStateOf(false) }
    val contentScale = if (isSuccess) ContentScale.Crop else ContentScale.None
    val colorFilter = if (isSuccess) {
        null
    } else {
        ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
    }

    AsyncImage(
        modifier = Modifier
            .height(200.dp)
            .aspectRatio(ratio = 2f / 3f, matchHeightConstraintsFirst = true)
            .clip(MaterialTheme.shapes.medium)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .then(modifier),
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        placeholder = painterResource(R.drawable.ic_downloading),
        error = painterResource(R.drawable.ic_error),
        fallback = painterResource(R.drawable.ic_image),
        onSuccess = { isSuccess = true },
        contentScale = contentScale,
        colorFilter = colorFilter,
    )
}