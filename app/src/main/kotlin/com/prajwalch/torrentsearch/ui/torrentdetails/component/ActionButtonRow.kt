package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun ActionButtonRow(
    onOpenMagnetLink: () -> Unit,
    onDownloadTorrentFile: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookmarkButton(isBookmarked = isBookmarked, onToggleBookmark = onToggleBookmark)
        DownloadTorrentFileButton(onClick = onDownloadTorrentFile)

        Spacer(Modifier.width(MaterialTheme.spaces.extraSmall))
        // "Open magnet link"
        Button(
            onClick = onOpenMagnetLink,
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
        FilledTonalIconButton(modifier = modifier, onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = buttonText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkButton(
    isBookmarked: Boolean,
    onToggleBookmark: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tooltipPositionProvider =
        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)
    val buttonText = stringResource(R.string.torrent_details_button_bookmark_torrent)
    val buttonIconResId = if (isBookmarked) R.drawable.ic_star_filled else R.drawable.ic_star

    TooltipBox(
        positionProvider = tooltipPositionProvider,
        tooltip = { PlainTooltip { Text(text = buttonText) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconToggleButton(
            modifier = modifier,
            checked = isBookmarked,
            onCheckedChange = onToggleBookmark,
        ) {
            Icon(
                painter = painterResource(buttonIconResId),
                contentDescription = buttonText,
            )
        }
    }
}

@Preview
@Composable
private fun ActionButtonRowPreview() {
    TorrentSearchTheme(darkTheme = true) {
        ActionButtonRow(
            onOpenMagnetLink = {},
            onDownloadTorrentFile = {},
            isBookmarked = false,
            onToggleBookmark = {},
        )
    }
}