package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults
import com.prajwalch.torrentsearch.ui.component.TryAgainButton
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme

@Composable
fun SomethingWentWrongState(
    message: String?,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text = stringResource(R.string.torrent_details_state_something_wrong_title),
                color = MaterialTheme.colorScheme.error,
            )
        },
        description = message?.let {
            { Text(text = it, textAlign = TextAlign.Center) }
        },
        primaryAction = { TryAgainButton(onClick = onTryAgain) },
    )
}

@Preview
@Composable
private fun SomethingWentWrongStatePreview() {
    TorrentSearchTheme {
        SomethingWentWrongState(
            message = "SearchProviderException in [one=1, two=2]: Unable to connect to https://example.com",
            onTryAgain = {},
        )
    }
}