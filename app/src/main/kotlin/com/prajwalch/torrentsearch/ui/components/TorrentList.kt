package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prajwalch.torrentsearch.data.Torrent

@Composable
fun TorrentList(torrents: List<Torrent>, onClick: (Torrent) -> Unit) {
    LazyColumn {
        items(torrents) { TorrentListItem(it, onClick = { onClick(it) }) }
    }
}

@Composable
private fun TorrentListItem(torrent: Torrent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Text(torrent.name, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        TorrentMetadataInfo(torrent)
    }
    HorizontalDivider()
}

@Composable
private fun TorrentMetadataInfo(torrent: Torrent) {
    val color = Color.Gray
    val sizeAndSeedsBg = Color.Gray.copy(alpha = 0.1f)
    val fontSize = 11.sp
    val spaceWidth = Modifier.width(8.dp)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Badge(
            text = torrent.size.toString(),
            icon = Icons.Default.Info,
            color = color,
            fontSize = fontSize,
            background = sizeAndSeedsBg
        )
        Spacer(spaceWidth)
        Badge(
            text = "Seeds ${torrent.seeds}",
            icon = Icons.Default.Share,
            color = color,
            fontSize = fontSize,
            background = sizeAndSeedsBg
        )
        Spacer(spaceWidth)
        Badge(
            text = torrent.providerName,
            color = color,
            fontSize = fontSize,
            background = Color.Gray.copy(alpha = 0.4f)
        )
    }
}
