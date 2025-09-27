package com.prajwalch.torrentsearch.ui.searchresults

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.NoInternetConnection
import com.prajwalch.torrentsearch.ui.components.ResultsNotFound
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SortMenu
import com.prajwalch.torrentsearch.ui.components.TorrentList

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onResultSelect: (Torrent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SearchResultsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val isFirstResultVisible by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex <= 1 }
    }
    val showScrollToTopButton = uiState.results.isNotEmpty() && !isFirstResultVisible

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SearchResultsScreenTopBar(
                onNavigateBack = onNavigateBack,
                onNavigateToSettings = onNavigateToSettings,
                filterQuery = uiState.filterQuery,
                onFilterQueryChange = viewModel::changeFilterQuery,
                scrollBehavior = scrollBehavior,
                currentSortCriteria = uiState.currentSortCriteria,
                currentSortOrder = uiState.currentSortOrder,
                onSortRequest = viewModel::sortResults,
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = uiState.isSearching,
            ) {
                LinearProgressIndicator()
            }

            SearchResultsScreenContent(
                modifier = Modifier.fillMaxHeight(),
                searchQuery = uiState.searchQuery,
                results = uiState.results,
                resultsNotFound = uiState.resultsNotFound,
                onResultSelect = onResultSelect,
                lazyListState = lazyListState,
                isLoading = uiState.isLoading,
                isInternetError = uiState.isInternetError,
                onRetry = { viewModel.performSearch() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsScreenTopBar(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    filterQuery: String,
    onFilterQueryChange: (String) -> Unit,
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortRequest: (SortCriteria, SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    var showSearchBar by remember { mutableStateOf(false) }
    val searchBarFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            searchBarFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = showSearchBar) {
        showSearchBar = false
    }

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            NavigateBackIconButton(onClick = {
                if (showSearchBar) showSearchBar = false else onNavigateBack()
            })
        },
        title = {
            if (showSearchBar) {
                SearchBar(
                    modifier = Modifier.focusRequester(searchBarFocusRequester),
                    query = filterQuery,
                    onQueryChange = onFilterQueryChange,
                )
            }
        },
        actions = {
            if (!showSearchBar) {
                IconButton(onClick = { showSearchBar = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                    )
                }

                Box {
                    var showSortMenu by remember(currentSortCriteria, currentSortOrder) {
                        mutableStateOf(false)
                    }

                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sort),
                            contentDescription = stringResource(R.string.button_open_sort_options),
                        )
                    }

                    SortMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        currentSortCriteria = currentSortCriteria,
                        currentSortOrder = currentSortOrder,
                        onSortRequest = onSortRequest,
                    )
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.button_go_to_settings_screen),
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        modifier = modifier.height(TextFieldDefaults.MinHeight),
        value = query,
        onValueChange = onQueryChange,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(text = stringResource(R.string.search_results)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.desc_clear_search_query),
                    )
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SearchResultsScreenContent(
    searchQuery: String,
    results: List<Torrent>,
    resultsNotFound: Boolean,
    onResultSelect: (Torrent) -> Unit,
    lazyListState: LazyListState,
    isLoading: Boolean,
    isInternetError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            isLoading -> LoadingIndicator(modifier = Modifier.fillMaxSize())

            isInternetError && results.isEmpty() -> NoInternetConnection(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )

            resultsNotFound -> ResultsNotFound(modifier = Modifier.fillMaxSize())

            results.isNotEmpty() -> {
                TorrentList(
                    torrents = results,
                    onTorrentSelect = onResultSelect,
                    toolbarContent = {
                        Text(
                            text = stringResource(
                                R.string.hint_results_count,
                                results.size,
                                searchQuery,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    lazyListState = lazyListState,
                )
            }
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