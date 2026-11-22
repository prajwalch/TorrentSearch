package com.prajwalch.torrentsearch.ui.bookmarks.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onDeleteBookmark: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LazyColumnWithScrollbar(
        modifier = modifier,
        state = lazyListState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(
            items = bookmarks,
            key = { _, bookmark -> bookmark.id },
            contentType = { _, bookmark -> bookmark.category },
        ) { index, bookmark ->
            BookmarkListItem(
                modifier = Modifier.animateItem(),
                bookmark = bookmark,
                onClick = { onBookmarkClick(bookmark) },
                onDelete = { onDeleteBookmark(bookmark.id) },
                shapes = ListItemDefaults.segmentedShapes(index, bookmarks.size),
            )
        }
    }
}

@Composable
private fun BookmarkListItem(
    bookmark: Torrent,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val coroutineScope = rememberCoroutineScope()

    val thresholdReached = swipeToDismissBoxState.targetValue != SwipeToDismissBoxValue.Settled
    val backgroundContentColor by animateColorAsState(
        if (thresholdReached) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        }
    )
    val deleteIconColor by animateColorAsState(
        if (thresholdReached) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
        }
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = swipeToDismissBoxState,
        backgroundContent = {
            Icon(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = backgroundContentColor,
                        shape = shapes.shape,
                    )
                    .wrapContentSize(align = Alignment.CenterEnd)
                    .padding(horizontal = MaterialTheme.spaces.large),
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = deleteIconColor,
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
            onClick = onClick,
            name = bookmark.name,
            size = bookmark.size,
            seeders = bookmark.seeders,
            peers = bookmark.peers,
            uploadDate = bookmark.uploadDate,
            category = bookmark.category,
            providerName = bookmark.providerName,
            isNSFW = bookmark.isNSFW,
            shapes = shapes,
        )
    }
}