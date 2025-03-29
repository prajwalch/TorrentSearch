package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        Text(torrent.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        TorrentMetadataInfo(torrent)
    }
    HorizontalDivider()
}

@Composable
private fun TorrentMetadataInfo(torrent: Torrent) {
    val fontSize = 14.sp

    Row(modifier = Modifier.fillMaxWidth()) {
        Text("Size ${torrent.size}", fontSize = fontSize)
        Text("↑ Seeds: ${torrent.seeds} ↓ Peers: ${torrent.peers}", fontSize = fontSize)
        Badge(torrent.providerName)
    }
}
