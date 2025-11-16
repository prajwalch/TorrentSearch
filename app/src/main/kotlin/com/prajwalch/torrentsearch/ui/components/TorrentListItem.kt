package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun TorrentListItem(torrent: Torrent, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        overlineContent = { Text(text = torrent.uploadDate) },
        headlineContent = { Text(text = torrent.name) },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.spaces.extraSmall,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                TorrentMetadata(
                    size = torrent.size,
                    seeders = torrent.seeders,
                    peers = torrent.peers,
                )
                BadgesRow {
                    torrent.category?.let { CategoryBadge(category = it) }
                    SearchProviderBadge(name = torrent.providerName)
                    if (torrent.isNSFW()) NSFWBadge()
                }
            }
        },
    )
    HorizontalDivider()
}

@Composable
private fun TorrentMetadata(
    size: String,
    seeders: UInt,
    peers: UInt,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.extraSmall,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TorrentMetadataText(text = size)
        BulletPoint()
        TorrentMetadataText(text = stringResource(R.string.torrent_list_seeders_format, seeders))
        BulletPoint()
        TorrentMetadataText(text = stringResource(R.string.torrent_list_peers_format, peers))
    }
}

@Composable
private fun TorrentMetadataText(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun BulletPoint(modifier: Modifier = Modifier) {
    Text(modifier = modifier, text = "\u2022")
}