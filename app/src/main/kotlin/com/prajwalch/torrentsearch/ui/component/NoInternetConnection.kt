package com.prajwalch.torrentsearch.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.prajwalch.torrentsearch.R

@Composable
fun NoInternetConnection(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        icon = R.drawable.ic_signal_wifi_off,
        title = R.string.internet_connection_error,
        actions = { TryAgainButton(onClick = onTryAgain) }
    )
}