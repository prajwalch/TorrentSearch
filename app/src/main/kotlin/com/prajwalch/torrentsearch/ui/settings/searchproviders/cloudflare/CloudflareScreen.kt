package com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component.ChallengeSolvedState
import com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component.ChallengeSolvingState
import com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component.CloudflareWebView

@Composable
fun CloudflareScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudflareViewModel = hiltViewModel(),
) {
    var isChallengeSolved by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CloudflareScreenTopBar(onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (!isChallengeSolved) {
                CloudflareWebView(
                    url = viewModel.url,
                    onChallengeSolved = {
                        viewModel.markProviderAsUnlocked()
                        isChallengeSolved = true
                    },
                )
            }

            AnimatedContent(isChallengeSolved) { challengeSolved ->
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
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        title = {},
    )
}