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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.SortCriteria
import com.prajwalch.torrentsearch.domain.SortOrder
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent

import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun TorrentList(
    torrents: List<Torrent>,
    onTorrentSelect: (Torrent) -> Unit,
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortTorrents: (SortCriteria, SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var showSortOptions by remember(torrents) { mutableStateOf(false) }

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
                            onClick = { showSortOptions = true },
                            currentSortCriteria = currentSortCriteria,
                            currentSortOrder = currentSortOrder,
                            onSortOrderChange = { onSortTorrents(currentSortCriteria, it) },
                        )
                        SortOptionsMenu(
                            expanded = showSortOptions,
                            onDismissRequest = { showSortOptions = false },
                            selectedSortCriteria = currentSortCriteria,
                            onSortCriteriaChange = { onSortTorrents(it, currentSortOrder) },
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
}

@Composable
fun TorrentListItem(torrent: Torrent, modifier: Modifier = Modifier) {
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
        headlineContent = {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = torrent.name,
            )
        },
        supportingContent = {
            TorrentMetadataInfo(
                size = torrent.size,
                seeders = torrent.seeders,
                peers = torrent.peers,
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
        category?.let {
            Text(
                text = it.toString(),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun TorrentProviderNameAndUploadDate(
    provider: String,
    uploadDate: String,
    modifier: Modifier = Modifier,
    contentSpace: Dp = 4.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = contentSpace),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = provider,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text("\u2022")
        Text(
            text = uploadDate,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TorrentMetadataInfo(
    size: String,
    seeders: UInt,
    peers: UInt,
    modifier: Modifier = Modifier,
    contentSpace: Dp = 8.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = contentSpace),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TorrentMetaInfo(
            leadingIconId = R.drawable.ic_size_info,
            text = size,
        )
        TorrentMetaInfo(
            leadingIconId = R.drawable.ic_seeders,
            text = stringResource(R.string.seeders, seeders),
        )
        TorrentMetaInfo(
            leadingIconId = R.drawable.ic_peers,
            text = stringResource(R.string.peers, peers),
        )
    }
}

@Composable
private fun TorrentMetaInfo(
    @DrawableRes
    leadingIconId: Int,
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = 8.dp,
        vertical = 4.dp,
    ),
    contentSpacing: Dp = 4.dp,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(space = contentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(size = 16.dp),
            painter = painterResource(id = leadingIconId),
            contentDescription = null,
        )
        Text(text = text)
    }
}