package com.prajwalch.torrentsearch.ui.bookmarks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.DeleteForeverIconButton
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SearchBar
import com.prajwalch.torrentsearch.ui.components.SearchIconButton
import com.prajwalch.torrentsearch.ui.components.SettingsIconButton
import com.prajwalch.torrentsearch.ui.components.SortDropdownMenu
import com.prajwalch.torrentsearch.ui.components.SortIconButton
import com.prajwalch.torrentsearch.ui.components.TorrentList

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBookmarkClick: (Torrent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<BookmarksViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val textFieldState = rememberTextFieldState()
    val searchBarFocusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showSearchBar by remember { mutableStateOf(false) }
    var showSortMenu by remember(uiState.currentSortCriteria, uiState.currentSortOrder) {
        mutableStateOf(false)
    }
    val showScrollToTopButton by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 1 }
    }

    val topBarTitle: @Composable () -> Unit = @Composable {
        if (showSearchBar) {
            SearchBar(
                modifier = Modifier.focusRequester(searchBarFocusRequester),
                textFieldState = textFieldState,
                placeholder = { Text(text = stringResource(R.string.search_bookmarks)) },
            )
        } else {
            Column {
                Text(text = stringResource(R.string.bookmarks_screen_title))

                if (uiState.bookmarks.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.bookmarks_count, uiState.bookmarks.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
    val topBarActions: @Composable RowScope.() -> Unit = @Composable {
        if (!showSearchBar && uiState.bookmarks.isNotEmpty()) {
            SearchIconButton(onClick = { showSearchBar = true })
            SortIconButton(onClick = { showSortMenu = true })
            SortDropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                currentSortCriteria = uiState.currentSortCriteria,
                currentSortOrder = uiState.currentSortOrder,
                onSortRequest = viewModel::sortBookmarks,
            )
            DeleteForeverIconButton(
                onClick = { viewModel.deleteAllBookmarks() },
                contentDescription = R.string.button_delete_all_bookmarks,
            )
        }
        SettingsIconButton(onClick = onNavigateToSettings)
    }

    BackHandler(enabled = showSearchBar) {
        showSearchBar = false
    }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            searchBarFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }
            .collectLatest { viewModel.filterBookmarks(query = it.toString()) }
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
                navigationIcon = {
                    ArrowBackIconButton(
                        onClick = onNavigateBack,
                        contentDescription = R.string.button_go_back_to_search_screen,
                    )
                },
                actions = topBarActions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ScrollToTopFAB(
                visible = showScrollToTopButton,
                onClick = {
                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.bookmarks.isEmpty()) {
            EmptyPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                title = R.string.msg_no_bookmarks,
            )
        } else {
            val bookmarks = uiState.filteredBookmarks ?: uiState.bookmarks

            TorrentList(
                modifier = Modifier.consumeWindowInsets(innerPadding),
                torrents = bookmarks,
                onTorrentClick = onBookmarkClick,
                contentPadding = innerPadding,
                lazyListState = lazyListState,
            )
        }
    }
}