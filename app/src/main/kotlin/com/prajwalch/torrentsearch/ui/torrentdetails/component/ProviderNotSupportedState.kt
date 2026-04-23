package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults

@Composable
fun ProviderNotSupportedState(providerName: String, modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.SmallIconSize),
                painter = painterResource(R.drawable.ic_error_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                stringResource(
                    R.string.torrent_details_error_provider_not_supported,
                    providerName
                ),
            )
        },
    )
}