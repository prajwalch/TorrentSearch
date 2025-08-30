package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SortMenu
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.BookmarksViewModel

import kotlinx.coroutines.launch

@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTorrentSelect: (Torrent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel = activityScopedViewModel<BookmarksViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val showDeleteAllAction by remember {
        derivedStateOf { uiState.bookmarks.isNotEmpty() }
    }
    // Scroll to top button related.
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val showScrollToTopButton by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 1 }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            BookmarksScreenTopBar(
                onNavigateBack = onNavigateBack,
                onDeleteAllBookmarks = viewModel::deleteAllBookmarks,
                onNavigateToSettings = onNavigateToSettings,
                showDeleteAllAction = showDeleteAllAction,
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
                headlineId = R.string.msg_nothing_here_yet,
            )
        } else {
            TorrentList(
                modifier = Modifier.consumeWindowInsets(innerPadding),
                torrents = uiState.bookmarks,
                onTorrentSelect = onTorrentSelect,
                toolbarContent = {
                    SortMenu(
                        currentSortCriteria = uiState.currentSortCriteria,
                        currentSortOrder = uiState.currentSortOrder,
                        onSortRequest = viewModel::sortBookmarks,
                    )
                },
                contentPadding = innerPadding,
                lazyListState = lazyListState,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksScreenTopBar(
    onNavigateBack: () -> Unit,
    onDeleteAllBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    showDeleteAllAction: Boolean,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.bookmarks_screen_title)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_back_to_search_screen,
            )
        },
        actions = {
            AnimatedVisibility(visible = showDeleteAllAction) {
                IconButton(onClick = onDeleteAllBookmarks) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_forever),
                        contentDescription = stringResource(R.string.button_delete_all_bookmarks),
                    )
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.button_go_to_settings_screen),
                )
            }
        }
    )
}