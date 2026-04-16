package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.component.TryAgainButton

@Composable
fun ResultsNotFound(modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        icon = R.drawable.ic_results_not_found,
        title = R.string.search_no_results_message,
    )
}

@Composable
fun ResultsNotFound(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        icon = R.drawable.ic_results_not_found,
        title = R.string.search_no_results_message,
        actions = { TryAgainButton(onClick = onTryAgain) }
    )
}