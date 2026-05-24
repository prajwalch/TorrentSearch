package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults
import com.prajwalch.torrentsearch.ui.component.TryAgainButton

@Composable
fun DetailsUnavailableState(
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentState(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.IconSize),
                painter = painterResource(R.drawable.ic_results_not_found),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.torrent_details_state_unavailable_title)) },
        description = {
            Text(
                text = stringResource(R.string.torrent_details_state_unavailable_description),
                textAlign = TextAlign.Center,
            )
        },
        primaryAction = { TryAgainButton(onClick = onTryAgain) },
    )
}