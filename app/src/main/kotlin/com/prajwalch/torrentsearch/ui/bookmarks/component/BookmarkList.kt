package com.prajwalch.torrentsearch.ui.bookmarks.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.component.LazyColumnWithScrollbar
import com.prajwalch.torrentsearch.ui.component.TorrentListItem
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

@Composable
fun BookmarkList(
    bookmarks: List<Torrent>,
    onBookmarkClick: (Torrent) -> Unit,
    onDeleteBookmark: (Torrent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LazyColumnWithScrollbar(
        modifier = modifier,
        state = lazyListState,
        contentPadding = contentPadding,
    ) {
        items(items = bookmarks, key = { it.infoHash }, contentType = { it.category }) {
            BookmarkListItem(
                modifier = Modifier.animateItem(),
                bookmark = it,
                onClick = { onBookmarkClick(it) },
                onDelete = { onDeleteBookmark(it) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun BookmarkListItem(
    bookmark: Torrent,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val coroutineScope = rememberCoroutineScope()

    SwipeToDismissBox(
        modifier = modifier,
        state = swipeToDismissBoxState,
        backgroundContent = {
            Icon(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.errorContainer)
                    .wrapContentSize(align = Alignment.CenterEnd)
                    .padding(horizontal = MaterialTheme.spaces.large),
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            } else {
                coroutineScope.launch { swipeToDismissBoxState.reset() }
            }
        },
    ) {
        TorrentListItem(
            modifier = Modifier.clickable(onClick = onClick),
            name = bookmark.name,
            size = bookmark.size,
            seeders = bookmark.seeders,
            peers = bookmark.peers,
            uploadDate = bookmark.uploadDate,
            category = bookmark.category,
            providerName = bookmark.providerName,
            isNSFW = bookmark.isNSFW(),
        )
    }
}