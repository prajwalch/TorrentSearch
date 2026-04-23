package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.TryAgainButton

@Composable
fun ResultsNotFoundState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                painterResource(R.drawable.ic_results_not_found),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.search_no_results_message)) },
    )
}

@Composable
fun ResultsNotFoundState(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                painterResource(R.drawable.ic_results_not_found),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.search_no_results_message)) },
        action = { TryAgainButton(onClick = onTryAgain) }
    )
}