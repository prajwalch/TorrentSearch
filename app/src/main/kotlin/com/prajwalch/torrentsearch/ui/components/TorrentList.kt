package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.data.Torrent

@Composable
fun TorrentList(torrents: List<Torrent>, onClick: (Torrent) -> Unit) {
    LazyColumn {
        items(items = torrents, key = { it.hash }) {
            TorrentListItem(
                torrent = it,
                modifier = Modifier
                    .clickable { onClick(it) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun TorrentListItem(torrent: Torrent, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(torrent.name) },
        supportingContent = {
            TorrentMetadataInfo(
                torrent = torrent,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
    HorizontalDivider()
}

@Composable
private fun TorrentMetadataInfo(torrent: Torrent, modifier: Modifier = Modifier) {
    val color = Color.Gray
    val sizeAndSeedsBg = Color.Gray.copy(alpha = 0.1f)
    val spaceWidth = Modifier.width(8.dp)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Badge(
            text = torrent.size.toString(),
            icon = Icons.Default.Info,
            color = color,
            background = sizeAndSeedsBg,
        )
        Spacer(spaceWidth)
        Badge(
            text = "Seeds ${torrent.seeds}",
            icon = Icons.Default.Share,
            color = color,
            background = sizeAndSeedsBg,
        )
        Spacer(spaceWidth)
        Badge(
            text = torrent.providerName,
            color = color,
            background = Color.Gray.copy(alpha = 0.4f),
        )
    }
}