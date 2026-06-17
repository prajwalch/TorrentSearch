package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState

@Composable
fun ChallengeSolvingState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = { CircularProgressIndicator() },
        title = { Text(stringResource(R.string.search_providers_state_challenge_solving_title)) },
        description = {
            Text(
                text = stringResource(R.string.search_providers_state_challenge_solving_description),
                textAlign = TextAlign.Center,
            )
        },
    )
}