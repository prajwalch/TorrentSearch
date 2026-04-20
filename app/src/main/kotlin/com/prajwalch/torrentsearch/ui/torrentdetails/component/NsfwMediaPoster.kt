package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun NsfwMediaPoster(
    url: String,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
    reveal: Boolean = false,
    showTapToRevealHint: Boolean = false,
) {
    // Display reveal hint only after image loads.
    var imageLoaded by rememberSaveable { mutableStateOf(false) }
    val revealHintScrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        MediaPoster(
            modifier = Modifier
                .clickable(enabled = imageLoaded, onClick = onToggleReveal)
                .scrimOverlay(
                    color = revealHintScrimColor,
                    enabled = !reveal && showTapToRevealHint,
                ),
            url = url,
            onSuccess = remember { { imageLoaded = true } },
            enableBlur = !reveal,
        )

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = imageLoaded && showTapToRevealHint,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TapToRevealHint()
        }
    }
}

private fun Modifier.scrimOverlay(color: Color, enabled: Boolean): Modifier {
    if (!enabled) return this

    val drawWithContent = Modifier.drawWithContent {
        drawContent()
        // Overlay background to make reveal hint text readable.
        drawRect(color = color)
    }

    return this.then(drawWithContent)
}

@Composable
private fun TapToRevealHint(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        VisibilityOffIcon()
        Spacer(Modifier.height(MaterialTheme.spaces.small))
        TapToRevealText()
    }
}

@Composable
private fun VisibilityOffIcon(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)

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