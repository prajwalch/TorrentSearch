package com.prajwalch.torrentsearch.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prajwalch.torrentsearch.R

@Composable
fun ResultsNotFound(modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        overlineIconId = R.drawable.ic_results_not_found,
        headlineTextId = R.string.msg_no_results_found,
    )
}