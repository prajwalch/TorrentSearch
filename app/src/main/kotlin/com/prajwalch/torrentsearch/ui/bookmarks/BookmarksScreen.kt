package com.prajwalch.torrentsearch.ui.bookmarks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.copyText
import com.prajwalch.torrentsearch.ui.TorrentFileDownloadEffect
import com.prajwalch.torrentsearch.ui.bookmarks.component.BookmarkList
import com.prajwalch.torrentsearch.ui.bookmarks.component.BookmarksCount
import com.prajwalch.torrentsearch.ui.bookmarks.component.DeleteAllConfirmationDialog
import com.prajwalch.torrentsearch.ui.component.AnimatedScrollToTopFAB
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.component.CollapsibleSearchBar
import com.prajwalch.torrentsearch.ui.component.DeleteForeverIconButton
import com.prajwalch.torrentsearch.ui.component.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.component.SearchIconButton
import com.prajwalch.torrentsearch.ui.component.SortDropdownMenu
import com.prajwalch.torrentsearch.ui.component.SortIconButton
import com.prajwalch.torrentsearch.ui.component.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.component.rememberCollapsibleSearchBarState
import com.prajwalch.torrentsearch.ui.rememberTorrentListState

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
    val torrentListState = rememberTorrentListState(itemsCount = { uiState.bookmarks.size })

    var selectedBookmark by retain { mutableStateOf<Torrent?>(null) }
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
            showNSFWBadge = bookmark.isNSFW,
            onDeleteBookmark = {
                viewModel.deleteBookmarkedTorrent(torrent = bookmark)
            },
            onDownloadTorrent = {
                onDownloadTorrent(bookmark.magnetUri())
            },
            onDownloadTorrentFile = {
                val torrentFileName = bookmark.name.replace(' ', '-')

                if (bookmark.fileDownloadLink != null) {
                    viewModel.downloadTorrentFile(
                        url = bookmark.fileDownloadLink,
                        fileName = torrentFileName,
                    )
                } else {
                    viewModel.downloadTorrentFileUsingInfoHash(
                        infoHash = bookmark.infoHash,
                        fileName = torrentFileName,
                    )
                }
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
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onFilterBookmarks = viewModel::filterBookmarks,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksScreenTopBar(
    uiState: BookmarksUiState,
    onNavigateBack: () -> Unit,
    onFilterBookmarks: (String) -> Unit,
    onChangeSortCriteria: (SortCriteria) -> Unit,
    onChangeSortOrder: (SortOrder) -> Unit,
    onDeleteAllBookmarks: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val searchBarState = rememberCollapsibleSearchBarState(visibleOnInitial = false)

    val topBarTitle: @Composable () -> Unit = @Composable {
        CollapsibleSearchBar(
            state = searchBarState,
            onQueryChange = onFilterBookmarks,
            placeholder = { Text(text = stringResource(R.string.bookmarks_search_query_hint)) },
        )

        if (!searchBarState.isVisible) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(text = stringResource(R.string.bookmarks_screen_title))
                // Subtitle.
                AnimatedVisibility(visible = uiState.bookmarks.isNotEmpty()) {
                    BookmarksCount(
                        totalBookmarksCount = uiState.bookmarks.size,
                        currentBookmarksCount = uiState.bookmarks.size,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
    val topBarActions: @Composable RowScope.() -> Unit = @Composable {
        var showSortOptions by rememberSaveable(uiState.sortOptions) { mutableStateOf(false) }
        // Bookmark related actions refers to those actions that directly
        // operate on bookmark/s.
        val enableBookmarkRelatedActions =
            uiState.bookmarks.isNotEmpty() || !searchBarState.isTextBlank

        if (!searchBarState.isVisible) {
            SearchIconButton(
                onClick = { searchBarState.showSearchBar() },
                enabled = enableBookmarkRelatedActions,
            )
            SortIconButton(
                onClick = { showSortOptions = true },
                enabled = enableBookmarkRelatedActions,
            )
            SortDropdownMenu(
                expanded = showSortOptions,
                onDismissRequest = { showSortOptions = false },
                currentCriteria = uiState.sortOptions.criteria,
                onChangeCriteria = onChangeSortCriteria,
                currentOrder = uiState.sortOptions.order,
                onChangeOrder = onChangeSortOrder,
            )
            DeleteForeverIconButton(
                onClick = onDeleteAllBookmarks,
                contentDescription = R.string.bookmarks_action_delete_all,
                enabled = enableBookmarkRelatedActions,
            )
        }

        // Additional actions.
        Box {
            var showAdditionalActions by rememberSaveable { mutableStateOf(false) }

            IconButton(onClick = { showAdditionalActions = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = null,
                )
            }
            TopBarAdditionalActionsDropdownMenu(
                expanded = showAdditionalActions,
                onDismiss = { showAdditionalActions = false },
                onImportBookmarks = onImportBookmarks,
                onExportBookmarks = onExportBookmarks,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
    }

    TopAppBar(
        title = topBarTitle,
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        actions = topBarActions,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TopBarAdditionalActionsDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun actionWithDismiss(action: () -> Unit) = {
        action()
        onDismiss()
    }
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_action_import)) },
            onClick = actionWithDismiss(onImportBookmarks),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = stringResource(R.string.bookmarks_action_import),
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_action_export)) },
            onClick = actionWithDismiss(onExportBookmarks),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_upload),
                    contentDescription = stringResource(R.string.bookmarks_action_export),
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_action_settings)) },
            onClick = actionWithDismiss(onNavigateToSettings),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.bookmarks_action_settings),
                )
            },
        )
    }
}