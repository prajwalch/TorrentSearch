package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent

@Composable
fun TorrentList(
    torrents: List<Torrent>,
    onTorrentSelect: (Torrent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            items = torrents,
            key = { it.hashCode() },
            contentType = { it.category },
        ) {
            TorrentListItem(
                modifier = Modifier
                    .animateItem()
                    .clickable { onTorrentSelect(it) },
                torrent = it,
            )
        }
    }
}

@Composable
private fun TorrentListItem(torrent: Torrent, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        overlineContent = {
            TorrentListItemOverlineContent(
                modifier = Modifier.fillMaxWidth(),
                provider = torrent.providerName,
                uploadDate = torrent.uploadDate,
                category = torrent.category,
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
private fun TorrentListItemOverlineContent(
    provider: String,
    uploadDate: String,
    category: Category?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            TorrentProviderNameAndUploadDate(
                provider = provider,
                uploadDate = uploadDate,
            )

            val categoryIsNSFWOrNull = category?.isNSFW ?: true
            if (categoryIsNSFWOrNull) {
                NSFWTag()
            }
        }
        category?.let { Text(text = it.toString()) }
    }
}

@Composable
private fun TorrentProviderNameAndUploadDate(
    provider: String,
    uploadDate: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = provider,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("\u2022")
        Text(
            text = uploadDate,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NSFWTag(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "NSFW",
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Black,
    )
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
    val containerShape = RoundedCornerShape(percent = 100)
    val containerBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val containerPaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

    val contentSpacing = 4.dp
    val iconSize = 16.dp
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