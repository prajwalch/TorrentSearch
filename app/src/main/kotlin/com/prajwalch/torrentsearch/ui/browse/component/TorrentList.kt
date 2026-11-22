package com.prajwalch.torrentsearch.ui.browse.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.component.LazyColumnWithScrollbar
import com.prajwalch.torrentsearch.ui.component.TorrentListItem
import com.prajwalch.torrentsearch.ui.search.component.TorrentsCount
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TorrentList(
    torrents: ImmutableList<Torrent>,
    onTorrentClick: (Torrent) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewedTorrentIds: Set<String> = emptySet(),
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier,
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
            )
        },
    ) {
        LazyColumnWithScrollbar(
            contentPadding = PaddingValues(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            state = lazyListState,
        ) {
            item {
                TorrentsCount(torrents.size)
            }

            itemsIndexed(
                items = torrents,
                key = { _, torrent -> torrent.id },
                contentType = { _, torrent -> torrent.category }
            ) { index, torrent ->
                val isViewed = remember(viewedTorrentIds) { torrent.id in viewedTorrentIds }
                val listItemAlpha = if (isViewed) 0.6f else 1f

                TorrentListItem(
                    modifier = Modifier
                        .animateItem()
                        .graphicsLayer { alpha = listItemAlpha },
                    onClick = { onTorrentClick(torrent) },
                    name = torrent.name,
                    size = torrent.size,
                    seeders = torrent.seeders,
                    peers = torrent.peers,
                    uploadDate = torrent.uploadDate,
                    category = torrent.category,
                    providerName = torrent.providerName,
                    isNSFW = torrent.isNSFW,
                    shapes = ListItemDefaults.segmentedShapes(index, torrents.size),
                )
            }
        }
    }
}