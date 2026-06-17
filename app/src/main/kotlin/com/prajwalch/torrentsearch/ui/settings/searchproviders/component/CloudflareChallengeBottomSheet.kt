package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudflareChallengeBottomSheet(
    onDismiss: () -> Unit,
    solverUrl: String,
    onChallengeSolved: () -> Unit,
    modifier: Modifier = Modifier,
    webViewMaxHeight: Dp = 400.dp,
) {
    val sheetState = rememberModalBottomSheetState()
    var isChallengeSolved by rememberSaveable { mutableStateOf(false) }
    var showWebView by rememberSaveable(isChallengeSolved) { mutableStateOf(false) }

    LaunchedEffect(isChallengeSolved) {
        if (isChallengeSolved) {
            delay(1.seconds)
            sheetState.hide()
            onChallengeSolved()
        }
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spaces.large)
                .padding(bottom = MaterialTheme.spaces.large)
                .animateContentSize()
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.End),
                onClick = { showWebView = !showWebView },
                enabled = !isChallengeSolved,
            ) {
                val iconId = if (showWebView) {
                    R.drawable.ic_preview_off
                } else {
                    R.drawable.ic_preview
                }
                Icon(
                    painter = painterResource(iconId),
                    contentDescription = null,
                )
            }

            AnimatedVisibility(!isChallengeSolved) {
                BoxedCloudflareWebView(
                    url = solverUrl,
                    onChallengeSolved = { isChallengeSolved = true },
                    height = if (showWebView) webViewMaxHeight else 0.dp,
                )
            }

            AnimatedVisibility(!showWebView) {
                AnimatedContent(isChallengeSolved) { challengeSolved ->
                    if (challengeSolved) {
                        ChallengeSolvedState(Modifier.fillMaxWidth())
                    } else {
                        ChallengeSolvingState(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}