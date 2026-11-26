package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.minus
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.LazyColumnWithScrollbar
import com.prajwalch.torrentsearch.ui.component.TorrentListItem
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TorrentList(
    torrents: ImmutableList<Torrent>,
    onTorrentClick: (Torrent) -> Unit,
    searchQuery: String,
    searchCategory: Category,
    searchErrorsCount: Int,
    onShowErrors: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    hiddenTorrentsCount: Int,
    onShowAllResults: () -> Unit,
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
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
            )
        },
    ) {
        LazyColumnWithScrollbar(
            // Fill the box, so that pull to refresh also works when the list is empty.
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
                SearchResultsSummary(
                    resultsCount = torrents.size,
                    hiddenCount = hiddenTorrentsCount,
                    errorsCount = searchErrorsCount,
                    query = searchQuery,
                    category = searchCategory,
                    onShowErrors = onShowErrors,
                )
            }

            itemsIndexed(
                items = torrents,
                key = { _, torrent -> torrent.id },
                contentType = { _, torrent -> torrent.category },
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

        if (torrents.isEmpty() && hiddenTorrentsCount > 0) {
            NoExactMatchesState(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = MaterialTheme.spaces.large),
                hiddenTorrentsCount = hiddenTorrentsCount,
                onShowAllResults = onShowAllResults,
            )
        }
    }
}

@Composable
private fun SearchResultsSummary(
    resultsCount: Int,
    hiddenCount: Int,
    errorsCount: Int,
    query: String,
    category: Category,
    onShowErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val categoryName = categoryStringResource(category)
        val summary = if (hiddenCount > 0) {
            stringResource(
                R.string.search_results_count_with_hidden_format,
                resultsCount,
                query,
                categoryName,
                hiddenCount,
            )
        } else {
            stringResource(
                R.string.search_results_count_format,
                resultsCount,
                query,
                categoryName,
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = summary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        val buttonContentPadding =
            ButtonDefaults.TextButtonWithIconContentPadding - PaddingValues(vertical = 6.dp)

        AnimatedVisibility(visible = errorsCount > 0) {
            TextButton(
                onClick = onShowErrors,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                contentPadding = buttonContentPadding,
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = "$errorsCount",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}