package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme

private enum class ImageState {
    Loading,
    Error,
    Success
}

@Composable
fun NetworkImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    onLoading: (@Composable () -> Unit)? = null,
    onError: (@Composable () -> Unit)? = null,
    onSuccess: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier, propagateMinConstraints = true) {
        var imageState by remember { mutableStateOf(ImageState.Loading) }

        if (imageState != ImageState.Error) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                onState = { state ->
                    imageState = when (state) {
                        AsyncImagePainter.State.Empty -> ImageState.Loading
                        is AsyncImagePainter.State.Loading -> ImageState.Loading
                        is AsyncImagePainter.State.Error -> ImageState.Error
                        is AsyncImagePainter.State.Success -> ImageState.Success
                    }
                },
                alignment = alignment,
                contentScale = contentScale,
            )
        }

        Crossfade(
            modifier = Modifier.matchParentSize(),
            targetState = imageState,
        ) { targetImageState ->
            when (targetImageState) {
                ImageState.Loading -> {
                    onLoading?.invoke() ?: ImageLoadingIndicator(Modifier.fillMaxSize())
                }

                ImageState.Error -> {
                    onError?.invoke() ?: ImageLoadError(
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { imageState = ImageState.Loading },
                    )
                }

                ImageState.Success -> {
                    onSuccess?.invoke()
                }
            }
        }
    }
}

@Composable
private fun ImageLoadingIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                strokeWidth = 2.0.dp,
            )
        }
    }
}

@Composable
private fun ImageLoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.error,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = onRetry) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                )
            }

            Text(
                text = stringResource(R.string.torrent_details_error_image_load_failed),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Preview
@Composable
private fun NetworkImagePreview() {
    TorrentSearchTheme {
        NetworkImage(
            modifier = Modifier
                .height(280.dp)
                .aspectRatio(2f / 3f, matchHeightConstraintsFirst = true),
            model = "",
            contentDescription = null
        )
    }
}