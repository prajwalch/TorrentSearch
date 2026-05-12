package com.prajwalch.torrentsearch.ui.browse.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.component.LazyColumnWithScrollbar
import com.prajwalch.torrentsearch.ui.component.TorrentListItem

import kotlinx.collections.immutable.ImmutableList

@Composable
fun TorrentList(
    torrents: ImmutableList<Torrent>,
    onTorrentClick: (Torrent) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewedTorrentHashes: Set<String> = emptySet(),
    lazyListState: LazyListState = rememberLazyListState(),
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        LazyColumnWithScrollbar(state = lazyListState) {
            itemsIndexed(
                items = torrents,
                contentType = { _, torrent -> torrent.category },
            ) { index, torrent ->
                val isViewed = torrent.infoHash in viewedTorrentHashes
                val listItemAlpha = if (isViewed) 0.6f else 1f

                TorrentListItem(
                    modifier = Modifier
                        .animateItem()
                        .clickable { onTorrentClick(torrent) }
                        .graphicsLayer { alpha = listItemAlpha },
                    name = torrent.name,
                    size = torrent.size,
                    seeders = torrent.seeders,
                    peers = torrent.peers,
                    uploadDate = torrent.uploadDate,
                    category = torrent.category,
                    providerName = torrent.providerName,
                    isNSFW = torrent.isNSFW,
                )

                if (index != torrents.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}