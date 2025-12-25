package com.prajwalch.torrentsearch.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/** The state for a torrent list. */
class TorrentListState(
    val lazyListState: LazyListState,
    private val itemsCount: () -> Int,
) {
    private val firstItemNotVisible by derivedStateOf {
        lazyListState.firstVisibleItemIndex > 1
    }

    val showScrollTopButton: Boolean
        get() = (itemsCount() > 0) && firstItemNotVisible

    suspend fun scrollToTop() {
        lazyListState.animateScrollToItem(0)
    }
}

/** Create and remember [TorrentListState]. */
@Composable
fun rememberTorrentListState(
    itemsCount: () -> Int,
    lazyListState: LazyListState = rememberLazyListState(),
): TorrentListState = remember {
    TorrentListState(
        lazyListState = lazyListState,
        itemsCount = itemsCount,
    )
}