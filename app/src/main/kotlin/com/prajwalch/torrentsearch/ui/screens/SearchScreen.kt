package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.SortCriteria
import com.prajwalch.torrentsearch.data.repository.SortOrder
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.NoInternetConnection
import com.prajwalch.torrentsearch.ui.components.ResultsNotFound
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.SortMenu
import com.prajwalch.torrentsearch.ui.components.TopSearchBar
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Scroll to top button related.
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val isFirstResultNotVisible by remember {
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
                isSearching = uiState.isSearching,
            )
        },
        floatingActionButton = {
            ScrollToTopFAB(
                visible = isFirstResultNotVisible && !uiState.isLoading,
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
            currentSortCriteria = uiState.currentSortCriteria,
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
    isSearching: Boolean,
    modifier: Modifier = Modifier,
) {
    var searchBarExpanded by remember { mutableStateOf(false) }
    val searchBarHorizontalPadding by animateDpAsState(if (searchBarExpanded) 0.dp else 16.dp)

    val windowInsets = TopAppBarDefaults.windowInsets

    Column(
        modifier = Modifier
            .windowInsetsPadding(windowInsets)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TopSearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = searchBarHorizontalPadding),
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {
                searchBarExpanded = query.isEmpty()
                onSearch()
            },
            expanded = searchBarExpanded,
            onExpandChange = { searchBarExpanded = it },
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
                    searchBarExpanded = false
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
        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = isSearching,
        ) {
            LinearProgressIndicator()
        }
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
    val contentPadding = PaddingValues(start = 12.dp, end = 16.dp)

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
            contentPadding = contentPadding,
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
            contentPadding = contentPadding,
        )
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
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortResults: (SortCriteria, SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            isLoading -> LoadingIndicator(modifier = Modifier.fillMaxSize())

            isInternetError && results.isEmpty() -> NoInternetConnection(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )

            resultsNotFound -> {
                Spacer(modifier = Modifier.height(16.dp))
                ResultsNotFound(modifier = Modifier.fillMaxWidth())
            }

            results.isNotEmpty() -> TorrentList(
                torrents = results,
                onTorrentSelect = onResultSelect,
                toolbarContent = {
                    Text(
                        text = stringResource(R.string.hint_results_count, results.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    SortMenu(
                        currentSortCriteria = currentSortCriteria,
                        currentSortOrder = currentSortOrder,
                        onSortRequest = onSortResults,
                    )
                },
                lazyListState = lazyListState,
            )

            else -> EmptyPlaceholder(
                modifier = Modifier.fillMaxSize(),
                headlineId = R.string.msg_nothing_here_yet,
                supportingTextId = R.string.msg_start_searching,
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