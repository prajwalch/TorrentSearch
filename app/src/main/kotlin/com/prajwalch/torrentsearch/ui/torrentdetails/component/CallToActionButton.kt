package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun CallToActionButton(
    onOpenMagnetLink: () -> Unit,
    onDownloadTorrentFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadTorrentFileButton(onClick = onDownloadTorrentFile)
        OpenMagnetLinkButton(onClick = onOpenMagnetLink)
    }
}

@Composable
private fun OpenMagnetLinkButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            painter = painterResource(R.drawable.ic_magnet),
            contentDescription = null,
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.torrent_details_button_open_magnet_link))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadTorrentFileButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tooltipPositionProvider = TooltipDefaults
        .rememberTooltipPositionProvider(TooltipAnchorPosition.Above)
    val buttonText = stringResource(R.string.torrent_details_button_download_torrent_file)

    TooltipBox(
        positionProvider = tooltipPositionProvider,
        tooltip = { PlainTooltip { Text(text = buttonText) } },
        state = rememberTooltipState(),
    ) {
        OutlinedIconButton(modifier = modifier, onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = buttonText,
            )
        }
    }
}