package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.Torrent

@Composable
fun TorrentList(torrents: List<Torrent>, onClick: (Torrent) -> Unit) {
    LazyColumn {
        items(items = torrents) {
            TorrentListItem(
                torrent = it,
                modifier = Modifier.clickable { onClick(it) },
            )
        }
    }
}

@Composable
private fun TorrentListItem(torrent: Torrent, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        overlineContent = {
            Text(
                text = torrent.providerName,
                color = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(torrent.name) },
        supportingContent = {
            TorrentMetadataInfo(
                torrent = torrent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        },
    )
    HorizontalDivider()
}

@Composable
private fun TorrentMetadataInfo(torrent: Torrent, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TorrentMetaInfo(
            text = torrent.size.toString(),
            icon = R.drawable.ic_size_info,
        )
        TorrentMetaInfo(
            text = "Seeds ${torrent.seeds}",
            icon = R.drawable.ic_seeders,
        )
        TorrentMetaInfo(
            text = "Peers ${torrent.peers}",
            icon = R.drawable.ic_peers,
        )
    }
}

@Composable
private fun TorrentMetaInfo(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes
    icon: Int? = null,
) {
    val containerShape = RoundedCornerShape(size = 16.dp)
    val containerBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val containerPaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

    val contentSpacing = 4.dp
    val iconSize = 18.dp
    val textStyle = MaterialTheme.typography.labelLarge

    Row(
        modifier = modifier
            .background(color = containerBackgroundColor, shape = containerShape)
            .padding(containerPaddingValues),
        horizontalArrangement = Arrangement.spacedBy(space = contentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier.size(size = iconSize),
            )
        }
        Text(text = text, style = textStyle)
    }
}