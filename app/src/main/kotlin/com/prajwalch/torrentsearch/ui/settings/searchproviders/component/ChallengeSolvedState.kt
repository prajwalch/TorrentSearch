package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults

@Composable
fun ChallengeSolvedState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.IconSize),
                painter = painterResource(R.drawable.ic_shield_checked),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.search_providers_state_challenge_solved_title)) },
    )
}