package com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component.BoxedCloudflareWebView
import com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component.ChallengeSolvedState
import com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component.ChallengeSolvingState
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun CloudflareScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudflareViewModel = hiltViewModel(),
) {
    var isChallengeSolved by rememberSaveable { mutableStateOf(false) }
    var showWebView by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CloudflareScreenTopBar(
                onNavigateBack = onNavigateBack,
                onToggleWebViewVisibility = { showWebView = !showWebView },
                isWebViewVisible = showWebView,
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(MaterialTheme.spaces.large)
                .animateContentSize()
        ) {
            AnimatedVisibility(!isChallengeSolved) {
                BoxedCloudflareWebView(
                    url = viewModel.url,
                    onChallengeSolved = {
                        isChallengeSolved = true
                        viewModel.markProviderAsUnlocked()
                    },
                    height = if (showWebView) 400.dp else 0.dp,
                )
            }

            AnimatedContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                targetState = isChallengeSolved,
            ) { challengeSolved ->
                if (challengeSolved) {
                    ChallengeSolvedState(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateBack = onNavigateBack,
                    )
                } else {
                    ChallengeSolvingState(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudflareScreenTopBar(
    onNavigateBack: () -> Unit,
    onToggleWebViewVisibility: () -> Unit,
    isWebViewVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        title = {},
        actions = {
            val webViewVisibilityIcon = if (isWebViewVisible) {
                R.drawable.ic_preview_off
            } else {
                R.drawable.ic_preview
            }
            IconButton(onClick = onToggleWebViewVisibility) {
                Icon(
                    painter = painterResource(webViewVisibilityIcon),
                    contentDescription = null,
                )
            }
        }
    )
}