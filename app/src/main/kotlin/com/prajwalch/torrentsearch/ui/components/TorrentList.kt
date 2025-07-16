package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.viewmodel.SortKey
import com.prajwalch.torrentsearch.ui.viewmodel.SortOrder

@Composable
fun TorrentList(
    currentSortKey: SortKey,
    currentSortOrder: SortOrder,
    torrents: List<Torrent>,
    onSortTorrents: (SortKey, SortOrder) -> Unit,
    onTorrentSelect: (Torrent) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    var showSortOptions by remember(torrents) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.hint_results_count, torrents.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box {
                    SortButton(
                        currentSortKey = currentSortKey,
                        currentSortOrder = currentSortOrder,
                        onClick = { showSortOptions = true },
                        onSortOrderChange = { onSortTorrents(currentSortKey, it) },
                    )
                    SortOptionsMenu(
                        expanded = showSortOptions,
                        selectedKey = currentSortKey,
                        onDismissRequest = { showSortOptions = false },
                        onSortKeySelect = { onSortTorrents(it, currentSortOrder) },
                    )
                }
            }
        }
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
private fun TorrentMetadataInfo(torrent: Torrent, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TorrentMetaInfo(
            text = torrent.size,
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