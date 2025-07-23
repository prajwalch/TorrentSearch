package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.components.EmptySearchPlaceholder
import com.prajwalch.torrentsearch.ui.components.NoInternetConnectionMessage
import com.prajwalch.torrentsearch.ui.components.ResultsNotFoundMessage
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.TopSearchBar
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SortKey
import com.prajwalch.torrentsearch.ui.viewmodel.SortOrder

import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: SearchViewModel,
    onTorrentSelect: (Torrent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    // Scroll to top button related.
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val showScrollToTopButton by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 1 }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLoading) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SearchScreenTopBar(
                modifier = Modifier.fillMaxWidth(),
                query = uiState.query,
                onQueryChange = viewModel::changeQuery,
                onSearch = viewModel::performSearch,
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategoryChange = viewModel::changeCategory,
                histories = uiState.histories,
                onDeleteSearchHistory = viewModel::deleteSearchHistory,
                onNavigateToBookmarks = onNavigateToBookmarks,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
        floatingActionButton = {
            ScrollToTopFAB(
                visible = showScrollToTopButton,
                onClick = {
                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                },
            )
        }
    ) { innerPadding ->
        SearchScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            results = uiState.results,
            resultsNotFound = uiState.resultsNotFound,
            onResultSelect = onTorrentSelect,
            lazyListState = lazyListState,
            isLoading = uiState.isLoading,
            isInternetError = uiState.isInternetError,
            onRetry = viewModel::performSearch,
            currentSortKey = uiState.currentSortKey,
            currentSortOrder = uiState.currentSortOrder,
            onSortResults = viewModel::sortResults,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    categories: List<Category>,
    selectedCategory: Category,
    onCategoryChange: (Category) -> Unit,
    histories: List<SearchHistoryUiState>,
    onDeleteSearchHistory: (SearchHistoryId) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val windowInsets = TopAppBarDefaults.windowInsets

    Column(
        modifier = Modifier
            .windowInsetsPadding(windowInsets)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        TopSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {
                expanded = query.isEmpty()
                onSearch()
            },
            expanded = expanded,
            onExpandChange = { expanded = it },
            trailingIcon = {
                var showMoreMenu by remember { mutableStateOf(false) }

                Box {
                    SearchBarTrailingIcon(
                        isQueryEmpty = query.isEmpty(),
                        onClearQuery = { onQueryChange("") },
                        onMoreClick = { showMoreMenu = true },
                    )
                    MoreMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        onNavigateToBookmarks = onNavigateToBookmarks,
                        onNavigateToSettings = onNavigateToSettings,
                    )

                }
            },
        ) {
            SearchHistoryList(
                histories = histories,
                onSearchRequest = {
                    onQueryChange(it)
                    expanded = false
                    onSearch()
                },
                onQueryChangeRequest = onQueryChange,
                onDeleteRequest = onDeleteSearchHistory,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelect = { newCategory ->
                if (selectedCategory != newCategory) {
                    onCategoryChange(newCategory)
                    onSearch()
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
    }
}

@Composable
private fun SearchBarTrailingIcon(
    isQueryEmpty: Boolean,
    onClearQuery: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = !isQueryEmpty) {
            ClearQueryIconButton(onClick = onClearQuery)
        }
        MoreIconButton(onClick = onMoreClick)
    }
}

@Composable
private fun ClearQueryIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Clear,
            contentDescription = stringResource(R.string.desc_clear_search_query)
        )
    }
}

@Composable
private fun MoreIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vertical),
            contentDescription = stringResource(R.string.button_go_to_settings_screen),
        )
    }
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.medium,
    ) {
        DropdownMenuItem(
            onClick = {
                onDismissRequest()
                onNavigateToBookmarks()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_star_filled),
                    contentDescription = stringResource(R.string.button_go_to_bookmarks_screen),
                )
            },
            text = { Text(text = stringResource(R.string.bookmarks_screen_title)) },
        )
        DropdownMenuItem(
            onClick = {
                onDismissRequest()
                onNavigateToSettings()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.button_go_to_settings_screen),
                )
            },
            text = { Text(text = stringResource(R.string.settings_screen_title)) },
        )
    }
}

@Composable
private fun ScrollToTopFAB(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight },
        exit = fadeOut() + slideOutVertically { fullHeight -> fullHeight },
    ) {
        FloatingActionButton(onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up),
                contentDescription = stringResource(R.string.button_scroll_to_top)
            )
        }
    }
}

@Composable
private fun SearchScreenContent(
    results: List<Torrent>,
    resultsNotFound: Boolean,
    onResultSelect: (Torrent) -> Unit,
    lazyListState: LazyListState,
    isLoading: Boolean,
    isInternetError: Boolean,
    onRetry: () -> Unit,
    currentSortKey: SortKey,
    currentSortOrder: SortOrder,
    onSortResults: (SortKey, SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.fillMaxSize())
        }

        if (isInternetError) {
            NoInternetConnectionMessage(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
        }

        if (resultsNotFound) {
            Spacer(modifier = Modifier.height(16.dp))
            ResultsNotFoundMessage()
        }

        if (!resultsNotFound && results.isEmpty()) {
            EmptySearchPlaceholder(modifier = Modifier.fillMaxSize())
        }

        if (results.isNotEmpty()) {
            TorrentList(
                torrents = results,
                onTorrentSelect = onResultSelect,
                currentSortKey = currentSortKey,
                currentSortOrder = currentSortOrder,
                onSortTorrents = onSortResults,
                lazyListState = lazyListState,
            )
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}