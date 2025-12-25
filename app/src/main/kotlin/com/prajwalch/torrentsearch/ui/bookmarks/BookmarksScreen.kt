package com.prajwalch.torrentsearch.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.extensions.copyText
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.CollapsibleSearchBar
import com.prajwalch.torrentsearch.ui.components.DeleteForeverIconButton
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.LazyColumnWithScrollbar
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SearchIconButton
import com.prajwalch.torrentsearch.ui.components.SettingsIconButton
import com.prajwalch.torrentsearch.ui.components.SortDropdownMenu
import com.prajwalch.torrentsearch.ui.components.SortIconButton
import com.prajwalch.torrentsearch.ui.components.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.components.TorrentListItem
import com.prajwalch.torrentsearch.ui.components.rememberCollapsibleSearchBarState
import com.prajwalch.torrentsearch.ui.rememberTorrentListState
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDownloadTorrent: (MagnetUri) -> Unit,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val torrentListState = rememberTorrentListState(itemsCount = { uiState.bookmarks.size })

    // Bookmark related states.
    var selectedBookmark by remember { mutableStateOf<Torrent?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    selectedBookmark?.let { bookmark ->
        val clipboard = LocalClipboard.current

        val magnetLinkCopiedMessage = stringResource(
            R.string.torrent_list_magnet_link_copied_message
        )
        val urlCopiedMessage = stringResource(
            R.string.torrent_list_url_copied_message,
        )

        TorrentActionsBottomSheet(
            onDismiss = { selectedBookmark = null },
            title = bookmark.name,
            showNSFWBadge = bookmark.isNSFW(),
            onDeleteBookmark = {
                viewModel.deleteBookmarkedTorrent(torrent = bookmark)
            },
            onDownloadTorrent = {
                onDownloadTorrent(bookmark.magnetUri())
            },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(text = bookmark.magnetUri())
                    snackbarHostState.showSnackbar(message = magnetLinkCopiedMessage)
                }
            },
            onShareMagnetLink = {
                onShareMagnetLink(bookmark.magnetUri())
            },
            onOpenDescriptionPage = {
                onOpenDescriptionPage(bookmark.descriptionPageUrl)
            },
            onCopyDescriptionPageUrl = {
                coroutineScope.launch {
                    clipboard.copyText(text = bookmark.descriptionPageUrl)
                    snackbarHostState.showSnackbar(message = urlCopiedMessage)
                }
            },
            onShareDescriptionPageUrl = {
                onShareDescriptionPageUrl(bookmark.descriptionPageUrl)
            },
            enableDescriptionPageActions = bookmark.descriptionPageUrl.isNotEmpty(),
        )
    }

    var showDeleteAllConfirmationDialog by remember { mutableStateOf(false) }
    if (showDeleteAllConfirmationDialog) {
        DeleteAllConfirmationDialog(
            onDismiss = { showDeleteAllConfirmationDialog = false },
            onConfirm = {
                viewModel.deleteAllBookmarks()
                showDeleteAllConfirmationDialog = false
            },
        )
    }

    val searchBarState = rememberCollapsibleSearchBarState(visibleOnInitial = false)
    var showSortOptions by remember(uiState.sortOptions) { mutableStateOf(false) }

    val topBarTitle: @Composable () -> Unit = @Composable {
        CollapsibleSearchBar(
            state = searchBarState,
            onQueryChange = viewModel::filterBookmarks,
            placeholder = { Text(text = stringResource(R.string.bookmarks_search_query_hint)) },
        )
        if (!searchBarState.isVisible) {
            Column {
                Text(text = stringResource(R.string.bookmarks_screen_title))

                if (uiState.bookmarks.isNotEmpty()) {
                    BookmarksCount(
                        totalBookmarksCount = uiState.bookmarks.size,
                        currentBookmarksCount = uiState.bookmarks.size,
                    )
                }
            }
        }
    }
    val topBarActions: @Composable RowScope.() -> Unit = @Composable {
        val isBookmarksNotEmpty = uiState.bookmarks.isNotEmpty()
        val isFilterQueryNotBlank by remember {
            derivedStateOf { searchBarState.textFieldState.text.isNotBlank() }
        }
        val enableBookmarksActions = isBookmarksNotEmpty || isFilterQueryNotBlank

        if (!searchBarState.isVisible) {
            SearchIconButton(
                onClick = { searchBarState.showSearchBar() },
                enabled = enableBookmarksActions,
            )
            SortIconButton(
                onClick = { showSortOptions = true },
                enabled = enableBookmarksActions,
            )
            SortDropdownMenu(
                expanded = showSortOptions,
                onDismissRequest = { showSortOptions = false },
                currentCriteria = uiState.sortOptions.criteria,
                onChangeCriteria = viewModel::setSortCriteria,
                currentOrder = uiState.sortOptions.order,
                onChangeOrder = viewModel::setSortOrder,
            )
            DeleteForeverIconButton(
                onClick = { showDeleteAllConfirmationDialog = true },
                contentDescription = R.string.bookmarks_action_delete_all,
                enabled = enableBookmarksActions,
            )
        }
        SettingsIconButton(onClick = onNavigateToSettings)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(modifier),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = topBarTitle,
                navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
                actions = topBarActions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ScrollToTopFAB(
                visible = torrentListState.showScrollTopButton,
                onClick = { coroutineScope.launch { torrentListState.scrollToTop() } },
            )
        },
    ) { innerPadding ->
        if (uiState.bookmarks.isEmpty()) {
            EmptyPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                title = R.string.bookmarks_empty_message,
            )
        } else {
            BookmarkList(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                bookmarks = uiState.bookmarks,
                onBookmarkClick = { selectedBookmark = it },
                onDeleteBookmark = viewModel::deleteBookmarkedTorrent,
                contentPadding = innerPadding,
                lazyListState = torrentListState.lazyListState,
            )
        }
    }
}

@Composable
private fun BookmarksCount(
    totalBookmarksCount: Int,
    currentBookmarksCount: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.labelMedium,
) {
    Text(
        modifier = modifier,
        text = pluralStringResource(
            R.plurals.bookmarks_count_format,
            totalBookmarksCount,
            currentBookmarksCount,
        ),
        color = color,
        style = style,
    )
}

@Composable
private fun DeleteAllConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete_forever),
                contentDescription = null,
            )
        },
        title = {
            Text(text = stringResource(R.string.bookmarks_dialog_title_delete_all))
        },
        text = {
            Text(text = stringResource(R.string.bookmarks_dialog_message_delete_all))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.bookmarks_button_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.button_cancel))
            }
        },
    )
}

@Composable
private fun BookmarkList(
    bookmarks: List<Torrent>,
    onBookmarkClick: (Torrent) -> Unit,
    onDeleteBookmark: (Torrent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    lazyListState: LazyListState,
) {
    LazyColumnWithScrollbar(
        modifier = modifier,
        state = lazyListState,
        contentPadding = contentPadding,
    ) {
        items(items = bookmarks, key = { it.id }, contentType = { it.category }) {
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
            torrent = bookmark,
        )
    }
}