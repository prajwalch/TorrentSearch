package com.prajwalch.torrentsearch.ui.searchproviders.component

import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChallengeSolvingState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = { ContainedLoadingIndicator() },
        title = { Text(stringResource(R.string.search_providers_state_challenge_solving_title)) },
        description = {
            Text(
                text = stringResource(R.string.search_providers_state_challenge_solving_description),
                textAlign = TextAlign.Center,
            )
        },
    )
}