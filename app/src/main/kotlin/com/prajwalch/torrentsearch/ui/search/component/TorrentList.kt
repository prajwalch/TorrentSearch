package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.minus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
    modifier: Modifier = Modifier,
    viewedTorrentIds: Set<String> = emptySet(),
    lazyListState: LazyListState = rememberLazyListState(),
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        LazyColumnWithScrollbar(
            state = lazyListState,
            contentPadding = PaddingValues(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            item {
                SearchResultsSummary(
                    resultsCount = torrents.size,
                    errorsCount = searchErrorsCount,
                    query = searchQuery,
                    category = searchCategory,
                    onShowErrors = onShowErrors,
                )
            }

            items(items = torrents, key = { it.id }, contentType = { it.category }) {
                val isViewed = remember(viewedTorrentIds) { it.id in viewedTorrentIds }
                val listItemAlpha = if (isViewed) 0.6f else 1f

                TorrentListItem(
                    modifier = Modifier
                        .animateItem()
                        .clickable { onTorrentClick(it) }
                        .graphicsLayer { alpha = listItemAlpha },
                    name = it.name,
                    size = it.size,
                    seeders = it.seeders,
                    peers = it.peers,
                    uploadDate = it.uploadDate,
                    category = it.category,
                    providerName = it.providerName,
                    isNSFW = it.isNSFW,
                )
            }
        }
    }
}

@Composable
private fun SearchResultsSummary(
    resultsCount: Int,
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
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(
                R.string.search_results_count_format,
                resultsCount,
                query,
                categoryStringResource(category),
            ),
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