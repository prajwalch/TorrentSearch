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
    revealed: Boolean = false,
    showTapToRevealHint: Boolean = false,
) {
    // Apply effects (clickable, blur and scrim) only after image loads successfully.
    var applyEffects by rememberSaveable { mutableStateOf(false) }

    // A semi-transparent black background which sits between the image
    // and tap-to-revel hint (if enabled).
    val scrimOverlay = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    val showScrimOverlay = !applyEffects || (!revealed && showTapToRevealHint)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        MediaPoster(
            modifier = Modifier
                .clickable(onClick = onToggleReveal, enabled = applyEffects)
                .scrimOverlay(scrim = scrimOverlay, enabled = showScrimOverlay),
            url = url,
            onSuccess = { applyEffects = true },
            enableBlur = !revealed,
        )

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = applyEffects && showTapToRevealHint,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TapToRevealHint()
        }
    }
}

private fun Modifier.scrimOverlay(scrim: Color, enabled: Boolean): Modifier {
    val drawModifier = if (!enabled) {
        Modifier
    } else {
        Modifier.drawWithContent {
            drawContent()
            drawRect(color = scrim)
        }
    }

    return this.then(drawModifier)
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