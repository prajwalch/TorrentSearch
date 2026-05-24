package com.prajwalch.torrentsearch.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R

@Composable
fun NoInternetConnectionState(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.IconSize),
                painter = painterResource(R.drawable.ic_signal_wifi_off),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.internet_connection_error)) },
        primaryAction = { TryAgainButton(onClick = onTryAgain) }
    )
}