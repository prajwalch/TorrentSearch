package com.prajwalch.torrentsearch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.SortCriteria
import com.prajwalch.torrentsearch.data.repository.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.NoInternetConnection
import com.prajwalch.torrentsearch.ui.components.ResultsNotFound
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SearchBar
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.SearchHistoryListItem
import com.prajwalch.torrentsearch.ui.components.SortMenu
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTorrentSelect: (Torrent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel = activityScopedViewModel<SearchViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val isFirstResultVisible by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex <= 1 }
    }
    val showScrollToTopButton = uiState.results.isNotEmpty() && !isFirstResultVisible

    BackHandler(enabled = uiState.isResettable()) {
        viewModel.resetToDefault()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ScrollToTopFAB(
                visible = showScrollToTopButton,
                onClick = {
                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.padding(
                    // TODO: Don't depend on `SearchBarDefaults` and remove
                    //       hardcoded padding too.
                    top = SearchBarDefaults.InputFieldHeight + 16.dp,
                ),
            ) {
                CategoryChipsRow(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = {
                        viewModel.changeCategory(it)
                        viewModel.performSearch()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = uiState.isSearching,
                ) {
                    LinearProgressIndicator()
                }

                SearchScreenContent(
                    modifier = Modifier.fillMaxSize(),
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

            TopSearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                query = uiState.query,
                onQueryChange = viewModel::changeQuery,
                onSearch = viewModel::performSearch,
                histories = uiState.histories,
                onDeleteSearchHistory = viewModel::deleteSearchHistory,
                onNavigateToBookmarks = onNavigateToBookmarks,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    histories: List<SearchHistoryUiState>,
    onDeleteSearchHistory: (SearchHistoryId) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val horizontalPadding by animateDpAsState(if (expanded) 0.dp else 16.dp)

    SearchBar(
        modifier = modifier.padding(horizontal = horizontalPadding),
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
            historyListItem = {
                SearchHistoryListItem(
                    modifier = Modifier
                        .animateItem()
                        .clickable {
                            onQueryChange(it.query)
                            expanded = false
                            onSearch()
                        },
                    query = it.query,
                    onInsertClick = { onQueryChange(it.query) },
                    onDeleteClick = { onDeleteSearchHistory(it.id) },
                )
            },
        )
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
            IconButton(onClick = onClearQuery) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.desc_clear_search_query),
                )
            }
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = stringResource(R.string.button_go_to_settings_screen),
            )
        }
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