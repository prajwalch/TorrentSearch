package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp

import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.transformations

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun NsfwPosterImage(
    url: String,
    modifier: Modifier = Modifier,
    height: Dp = PosterImageDefaults.Height,
    aspectRatio: Float = PosterImageDefaults.AspectRatio,
    shape: Shape = PosterImageDefaults.Shape,
    initialRevealed: Boolean = false,
) {
    var revealed by rememberSaveable(initialRevealed) { mutableStateOf(initialRevealed) }
    var showTapToRevealHint by rememberSaveable(initialRevealed) { mutableStateOf(!initialRevealed) }

    NetworkImage(
        modifier = modifier
            .height(height)
            .aspectRatio(ratio = aspectRatio, matchHeightConstraintsFirst = true)
            .clip(shape)
            .border(border = PosterImageDefaults.BorderStroke, shape = shape)
            .clickable {
                if (showTapToRevealHint) showTapToRevealHint = false
                revealed = !revealed
            },
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(url)
            .apply { if (!revealed) transformations(BlurTransformation()) }
            .build(),
        contentDescription = null,
        onSuccess = {
            AnimatedVisibility(
                visible = showTapToRevealHint,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TapToRevealHint(Modifier.fillMaxSize())
            }
        },
    )
}

@Composable
private fun TapToRevealHint(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
) {
    Column(
        modifier = modifier.background(color = backgroundColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VisibilityOffIcon()
        Spacer(Modifier.height(MaterialTheme.spaces.small))
        TapToRevealText()
    }
}

@Composable
private fun VisibilityOffIcon(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
) {
    Icon(
        modifier = modifier
            .background(color = backgroundColor, shape = CircleShape)
            .padding(MaterialTheme.spaces.small),
        painter = painterResource(R.drawable.ic_visibility_off),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TapToRevealText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.torrent_details_tap_to_reveal_hint),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
}