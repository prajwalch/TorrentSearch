package com.prajwalch.torrentsearch.ui.searchproviders.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults

@Composable
fun ChallengeSolveErrorState(
    error: String,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.IconSize),
                painter = painterResource(R.drawable.ic_gpp_bad),
                contentDescription = null,
            )
        },
        title = {
            Text(stringResource(R.string.search_providers_state_challenge_solve_error_title))
        },
        description = {
            val description = buildString {
                append(stringResource(R.string.search_providers_state_challenge_solve_error_description))
                append(' ')
                append("[$error]")
            }
            Text(text = description, textAlign = TextAlign.Center)
        },
        primaryAction = {
            TextButton(onClick = onTryAgain) {
                Text(stringResource(R.string.search_providers_button_try_again))
            }
        },
    )
}