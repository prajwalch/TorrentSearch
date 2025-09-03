package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Torrent

import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun TorrentList(
    torrents: List<Torrent>,
    onTorrentSelect: (Torrent) -> Unit,
    modifier: Modifier = Modifier,
    toolbarContent: @Composable (RowScope.() -> Unit)? = null,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val scrollbarUnselectedColor = MaterialTheme.colorScheme.primary
    val scrollbarSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    LazyColumnScrollbar(
        state = lazyListState,
        settings = ScrollbarSettings.Default.copy(
            scrollbarPadding = 2.dp,
            thumbThickness = 8.dp,
            thumbMinLength = 0.07f,
            thumbUnselectedColor = scrollbarUnselectedColor,
            thumbSelectedColor = scrollbarSelectedColor,
            hideDelayMillis = 3000,
        ),
    ) {
        LazyColumn(
            modifier = modifier,
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            toolbarContent?.let { toolbar ->
                item {
                    TorrentListToolbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        content = toolbar,
                    )
                }
            }
            items(items = torrents, contentType = { it.category }) {
                TorrentListItem(
                    modifier = Modifier
                        .animateItem()
                        .clickable { onTorrentSelect(it) },
                    torrent = it,
                )
            }
        }
    }
}

@Composable
private fun TorrentListToolbar(
    modifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit),
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

@Composable
fun TorrentListItem(torrent: Torrent, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        overlineContent = { Text(text = torrent.uploadDate) },
        headlineContent = {
            Text(
                modifier = Modifier.padding(vertical = 4.dp),
                text = torrent.name,
            )
        },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
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
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TorrentMetadataText(text = size)
        BulletPoint()
        TorrentMetadataText(text = stringResource(R.string.seeders, seeders))
        BulletPoint()
        TorrentMetadataText(text = stringResource(R.string.peers, peers))
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