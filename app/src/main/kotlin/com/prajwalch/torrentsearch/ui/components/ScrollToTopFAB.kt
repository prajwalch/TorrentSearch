package com.prajwalch.torrentsearch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.R

@Composable
fun ScrollToTopFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight },
        exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight },
    ) {
        FloatingActionButton(modifier = Modifier.imePadding(), onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up),
                contentDescription = null,
            )
        }
    }
}