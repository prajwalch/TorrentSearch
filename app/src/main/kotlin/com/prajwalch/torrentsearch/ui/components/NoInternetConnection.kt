package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R

@Composable
fun NoInternetConnection(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        overlineIconId = R.drawable.ic_signal_wifi_off,
        headlineTextId = R.string.msg_no_internet_connection,
        actions = {
            Button(onClick = onRetry) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.desc_retry_connection),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.button_try_again))
            }
        }
    )
}