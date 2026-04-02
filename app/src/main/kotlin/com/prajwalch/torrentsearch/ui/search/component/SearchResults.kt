package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.LazyColumnWithScrollbar
import com.prajwalch.torrentsearch.ui.component.TorrentListItem
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.collections.immutable.ImmutableList

@Composable
fun SearchResults(
    searchResults: ImmutableList<Torrent>,
    onResultClick: (Torrent) -> Unit,
    searchQuery: String,
    searchCategory: Category,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    viewedIds: Set<String> = emptySet(),
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        LazyColumnWithScrollbar(state = lazyListState) {
            item {
                SearchResultsCount(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spaces.large,
                        vertical = MaterialTheme.spaces.small,
                    ),
                    searchResultsSize = searchResults.size,
                    searchQuery = searchQuery,
                    searchCategory = searchCategory,
                )
            }

            items(items = searchResults, contentType = { it.category }) {
                TorrentListItem(
                    modifier = Modifier
                        .animateItem()
                        .clickable { onResultClick(it) },
                    name = it.name,
                    size = it.size,
                    seeders = it.seeders,
                    peers = it.peers,
                    uploadDate = it.uploadDate,
                    category = it.category,
                    providerName = it.providerName,
                    isNSFW = it.isNSFW(),
                    isViewed = it.id in viewedIds,
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SearchResultsCount(
    searchResultsSize: Int,
    searchQuery: String,
    searchCategory: Category,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(
            R.string.search_results_count_format,
            searchResultsSize,
            searchQuery,
            categoryStringResource(searchCategory),
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}