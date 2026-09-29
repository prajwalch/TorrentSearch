package com.prajwalch.torrentsearch.ui.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.home.ProvidersCheckState
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun ProvidersCheckNotificationBar(
    state: ProvidersCheckState,
    onDismiss: () -> Unit,
    onNavigateToSearchProviders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val enableSwipe = state != ProvidersCheckState.Checking

    val transition = updateTransition(state)
    val containerColor by transition.animateColor {
        when (it) {
            ProvidersCheckState.Checking -> MaterialTheme.colorScheme.surfaceContainerHigh
            is ProvidersCheckState.Complete -> MaterialTheme.colorScheme.primaryContainer
        }
    }
    val contentColor by transition.animateColor {
        when (it) {
            ProvidersCheckState.Checking -> MaterialTheme.colorScheme.onSurface
            is ProvidersCheckState.Complete -> MaterialTheme.colorScheme.onPrimaryContainer
        }
    }

    val clickable = if (state is ProvidersCheckState.Complete) {
        state.protectionStatusResult.numLockedProviders > 0
    } else {
        false
    }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        backgroundContent = {},
        enableDismissFromStartToEnd = enableSwipe,
        enableDismissFromEndToStart = enableSwipe,
        onDismiss = { onDismiss() },
    ) {
        Surface(
            modifier = modifier,
            onClick = onNavigateToSearchProviders,
            enabled = clickable,
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.spaces.large),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Leading icon
                Crossfade(state) { targetState ->
                    when (targetState) {
                        ProvidersCheckState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                                strokeWidth = 2.0.dp,
                            )
                        }

                        is ProvidersCheckState.Complete -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    // Title
                    Crossfade(state) { targetState ->
                        val textResId = when (targetState) {
                            ProvidersCheckState.Checking -> R.string.home_status_providers_checking
                            is ProvidersCheckState.Complete -> R.string.home_status_providers_check_complete
                        }

                        Text(
                            text = stringResource(textResId),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    // Subtitle
                    Crossfade(state) { targetState ->
                        val subtitle = when (targetState) {
                            ProvidersCheckState.Checking -> {
                                stringResource(R.string.home_message_please_wait)
                            }

                            is ProvidersCheckState.Complete -> {
                                val protectionStatusResult = targetState.protectionStatusResult

                                stringResource(
                                    R.string.search_providers_state_protection_status_update_complete,
                                    protectionStatusResult.numUnlockedProviders,
                                    protectionStatusResult.numLockedProviders,
                                )
                            }
                        }

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                        )
                    }
                }

                // Trailing icon
                AnimatedVisibility(clickable) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}