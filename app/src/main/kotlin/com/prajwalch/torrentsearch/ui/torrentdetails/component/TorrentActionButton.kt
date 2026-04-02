package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R

@Composable
fun OpenMagnetButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(modifier = modifier, onClick = onClick) {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            painter = painterResource(R.drawable.ic_magnet),
            contentDescription = null,
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.torrent_details_button_open_magnet))
    }
}

@Composable
fun DownloadTorrentButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(modifier = modifier, onClick = onClick) {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            painter = painterResource(R.drawable.ic_download),
            contentDescription = null,
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.torrent_details_button_download_torrent))
    }
}