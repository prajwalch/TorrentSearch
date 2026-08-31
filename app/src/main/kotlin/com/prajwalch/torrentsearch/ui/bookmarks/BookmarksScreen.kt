package com.prajwalch.torrentsearch.ui.bookmarks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.BookmarkedTorrent
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.ui.TorrentFileDownloadEffect
import com.prajwalch.torrentsearch.ui.bookmarks.component.BookmarkList
import com.prajwalch.torrentsearch.ui.bookmarks.component.BookmarksScreenTopBar
import com.prajwalch.torrentsearch.ui.bookmarks.component.DeleteAllConfirmationDialog
import com.prajwalch.torrentsearch.ui.component.AnimatedScrollToTopFAB
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.MessageCard
import com.prajwalch.torrentsearch.ui.component.MessageType
import com.prajwalch.torrentsearch.ui.component.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.rememberTorrentListState
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenMagnetLink: (MagnetUri) -> Unit,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (url: String, providerName: String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torrentFileDownloadState by viewModel.torrentFileDownloadState.collectAsStateWithLifecycle()

    val contentResolver = LocalContext.current.contentResolver
    val bookmarksExportedFileChooser = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { fileUri ->
        fileUri
            ?.let(contentResolver::openInputStream)
            ?.let(viewModel::importBookmarks)
    }
    val bookmarksExportLocationChooser = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TorrentSearchConstants.BOOKMARKS_EXPORT_FILE_TYPE),
    ) { fileUri ->
        fileUri
            ?.let(contentResolver::openOutputStream)
            ?.let(viewModel::exportBookmarks)
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val torrentListState = rememberTorrentListState(itemsCount = {
        val bookmarksState = uiState.bookmarksState
        if (bookmarksState is BookmarksState.Ready) bookmarksState.bookmarks.size else 0
    })

    var selectedBookmark by retain { mutableStateOf<BookmarkedTorrent?>(null) }
    selectedBookmark?.let { bookmark ->
        val bookmarkId = bookmark.id
        val bookmark = bookmark.torrent

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
            showNSFWBadge = bookmark.isNSFW,
            onDeleteBookmark = { viewModel.deleteBookmarkById(bookmarkId) },
            onOpenMagnetLink = { onOpenMagnetLink(bookmark.magnetUri()) },
            onDownloadTorrentFile = {
                if (bookmark.fileDownloadLink != null) {
                    viewModel.downloadTorrentFile(
                        url = bookmark.fileDownloadLink,
                        fileName = bookmark.name,
                    )
                } else {
                    viewModel.downloadTorrentFileUsingInfoHash(
                        infoHash = bookmark.infoHash,
                        fileName = bookmark.name,
                    )
                }
            },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(text = bookmark.magnetUri())
                    snackbarHostState.showSnackbar(message = magnetLinkCopiedMessage)
                }
            },
            onShareMagnetLink = { onShareMagnetLink(bookmark.magnetUri()) },
            onOpenDescriptionPage = {
                bookmark.descriptionPageUrl?.let {
                    onOpenDescriptionPage(it, bookmark.providerName)
                }
            },
            onCopyDescriptionPageUrl = {
                bookmark.descriptionPageUrl?.let {
                    coroutineScope.launch {
                        clipboard.copyText(it)
                        snackbarHostState.showSnackbar(urlCopiedMessage)
                    }
                }
            },
            onShareDescriptionPageUrl = {
                bookmark.descriptionPageUrl?.let {
                    onShareDescriptionPageUrl(it)
                }
            },
            enableDescriptionPageActions = bookmark.descriptionPageUrl != null,
        )
    }

    var showDeleteAllConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteAllConfirmationDialog) {
        DeleteAllConfirmationDialog(
            onDismiss = { showDeleteAllConfirmationDialog = false },
            onConfirm = {
                viewModel.deleteAllBookmarks()
                showDeleteAllConfirmationDialog = false
            },
        )
    }

    TorrentFileDownloadEffect(
        onWrite = viewModel::writeTorrentFile,
        state = torrentFileDownloadState,
        events = viewModel.torrentFileDownloadEvents,
        snackbarHostState = snackbarHostState,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(modifier),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            BookmarksScreenTopBar(
                onNavigateBack = onNavigateBack,
                totalBookmarksCount = uiState.totalBookmarksCount,
                onFilterBookmarks = viewModel::filterBookmarks,
                sortOptions = uiState.sortOptions,
                onChangeSortCriteria = viewModel::setSortCriteria,
                onChangeSortOrder = viewModel::setSortOrder,
                onDeleteAllBookmarks = { showDeleteAllConfirmationDialog = true },
                onImportBookmarks = {
                    // When mime type is given it restricts other type of files
                    // from being selectable.
                    bookmarksExportedFileChooser.launch(
                        TorrentSearchConstants.BOOKMARKS_EXPORT_FILE_TYPE,
                    )
                },
                onExportBookmarks = {
                    // Takes file name to create on the selected location.
                    bookmarksExportLocationChooser.launch(
                        TorrentSearchConstants.BOOKMARKS_EXPORT_FILE_NAME,
                    )
                },
                onNavigateToSettings = onNavigateToSettings,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedScrollToTopFAB(
                visible = torrentListState.showScrollTopButton,
                onClick = { coroutineScope.launch { torrentListState.scrollToTop() } },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            modifier = Modifier.padding(innerPadding),
            targetState = uiState.bookmarksState,
            contentKey = { it::class },
        ) { bookmarksState ->
            when (bookmarksState) {
                BookmarksState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                BookmarksState.Empty -> {
                    ContentState(
                        modifier = Modifier.fillMaxSize(),
                        title = { Text(stringResource(R.string.bookmarks_empty_message)) },
                    )
                }

                BookmarksState.EmptyNoMatches -> {
                    ContentState(
                        modifier = Modifier.fillMaxSize(),
                        title = { Text(stringResource(R.string.bookmarks_no_bookmarks_matched)) },
                    )
                }

                is BookmarksState.Ready -> {
                    BookmarksScreenContent(
                        modifier = Modifier.fillMaxSize(),
                        bookmarks = bookmarksState.bookmarks,
                        onBookmarkClick = { selectedBookmark = it },
                        onDeleteBookmark = { viewModel.deleteBookmarkById(it.id) },
                        showSwipeDeleteTip = uiState.showSwipeDeleteTip,
                        onHideSwipeDeleteTip = { viewModel.hideSwipeToDeleteTip() },
                        lazyListState = torrentListState.lazyListState,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarksScreenContent(
    bookmarks: List<BookmarkedTorrent>,
    onBookmarkClick: (BookmarkedTorrent) -> Unit,
    onDeleteBookmark: (BookmarkedTorrent) -> Unit,
    showSwipeDeleteTip: Boolean,
    onHideSwipeDeleteTip: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    Column(modifier = modifier) {
        AnimatedVisibility(showSwipeDeleteTip) {
            MessageCard(
                modifier = Modifier.padding(MaterialTheme.spaces.large),
                onClose = onHideSwipeDeleteTip,
                messageType = MessageType.Tip,
                text = { Text(stringResource(R.string.bookmarks_swipe_delete_tip)) },
            )
        }

        BookmarkList(
            modifier = Modifier.fillMaxSize(),
            bookmarks = bookmarks,
            onBookmarkClick = onBookmarkClick,
            onDeleteBookmark = onDeleteBookmark,
            contentPadding = PaddingValues(MaterialTheme.spaces.large),
            lazyListState = lazyListState,
        )
    }
}