package com.prajwalch.torrentsearch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun TorrentListItem(
    name: String,
    size: String,
    seeders: UInt,
    peers: UInt,
    uploadDate: String,
    category: Category?,
    providerName: String,
    isNSFW: Boolean,
    modifier: Modifier = Modifier,
    isViewed: Boolean = false,
) {
    val contentAlpha = if (isViewed) 0.6f else 1f

    ListItem(
        modifier = modifier.alpha(contentAlpha),
        overlineContent = { Text(text = uploadDate) },
        headlineContent = {
            Text(
                text = name,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
            )
        },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.spaces.extraSmall,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                TorrentMetadata(
                    size = size,
                    seeders = seeders,
                    peers = peers,
                )
                BadgesRow {
                    category?.let { CategoryBadge(category = it) }
                    SearchProviderBadge(name = providerName)
                    if (isNSFW) NSFWBadge()
                }
            }
        },
    )
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