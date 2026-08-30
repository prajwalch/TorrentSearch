package com.prajwalch.torrentsearch.ui.searchproviders.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

private sealed interface ChallengeSolveState {
    data object Solving : ChallengeSolveState
    data object Solved : ChallengeSolveState
    data class Error(val error: ChallengeSolveError) : ChallengeSolveState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudflareChallengeBottomSheet(
    onDismiss: () -> Unit,
    solverUrl: String,
    onChallengeSolved: () -> Unit,
    modifier: Modifier = Modifier,
    webViewMaxHeight: Dp = 400.dp,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var challengeSolveState by remember {
        mutableStateOf<ChallengeSolveState>(ChallengeSolveState.Solving)
    }
    var showWebView by rememberSaveable(challengeSolveState) { mutableStateOf(false) }

    SideEffect(challengeSolveState) {
        if (challengeSolveState == ChallengeSolveState.Solved) {
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
                enabled = challengeSolveState == ChallengeSolveState.Solving,
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

            Box(
                modifier = Modifier.heightIn(min = webViewMaxHeight),
                contentAlignment = Alignment.Center,
            ) {
                val webViewAlpha by animateFloatAsState(if (showWebView) 1f else 0f)
                if (challengeSolveState == ChallengeSolveState.Solving) {
                    BoxedCloudflareWebView(
                        modifier = Modifier.alpha(webViewAlpha),
                        url = solverUrl,
                        onChallengeSolved = { challengeSolveState = ChallengeSolveState.Solved },
                        onError = { challengeSolveState = ChallengeSolveState.Error(it) },
                        height = webViewMaxHeight,
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = !showWebView,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    AnimatedContent(targetState = challengeSolveState) { solveState ->
                        when (solveState) {
                            ChallengeSolveState.Solving -> {
                                ChallengeSolvingState(Modifier.fillMaxWidth())
                            }

                            ChallengeSolveState.Solved -> {
                                ChallengeSolvedState(Modifier.fillMaxWidth())
                            }

                            is ChallengeSolveState.Error -> {
                                ChallengeSolveErrorState(
                                    modifier = Modifier.fillMaxWidth(),
                                    error = solveState.error.displayName(),
                                    onTryAgain = {
                                        challengeSolveState = ChallengeSolveState.Solving
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ChallengeSolveError.displayName(): String = when (this) {
    ChallengeSolveError.BadUrl -> "Bad URL"
    ChallengeSolveError.ConnectFailed -> "Connect failed"
    ChallengeSolveError.HostLookupFailed -> "Host lookup failed"
    ChallengeSolveError.Timeout -> "Timeout"
    ChallengeSolveError.TooManyRedirects -> "Too many redirects"
    ChallengeSolveError.Unknown -> "Unknown error"
    is ChallengeSolveError.ApplicationError -> "Application error (${this.errorCode})"
}