package com.prajwalch.torrentsearch.ui.searchresults

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.NoInternetConnection
import com.prajwalch.torrentsearch.ui.components.ResultsNotFound
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SearchBar
import com.prajwalch.torrentsearch.ui.components.SettingsIconButton
import com.prajwalch.torrentsearch.ui.components.SortDropdownMenu
import com.prajwalch.torrentsearch.ui.components.SortIconButton
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.theme.spaces

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
    val showScrollToTopButton = uiState.searchResults.isNotEmpty() && !isFirstResultVisible

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            SearchResultsScreenTopBar(
                onNavigateBack = onNavigateBack,
                onNavigateToSettings = onNavigateToSettings,
                currentSortCriteria = uiState.currentSortCriteria,
                currentSortOrder = uiState.currentSortOrder,
                onSortRequest = viewModel::sortResults,
                filterOptions = uiState.filterOptions,
                onFilterQueryChange = viewModel::changeFilterQuery,
                onSearchProviderClick = viewModel::showSearchProviderResults,
                onShowZeroSeedersResultsClick = viewModel::showZeroSeedersResults,
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
                searchResults = uiState.searchResults,
                filteredResults = uiState.filteredResults,
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
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortRequest: (SortCriteria, SortOrder) -> Unit,
    filterOptions: FilterOptionsUiState,
    onFilterQueryChange: (String) -> Unit,
    onSearchProviderClick: (SearchProviderId) -> Unit,
    onShowZeroSeedersResultsClick: () -> Unit,
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

    var showFilterOptionBottomSheet by remember { mutableStateOf(false) }

    if (showFilterOptionBottomSheet) {
        FilterOptionsBottomSheet(
            onDismissRequest = { showFilterOptionBottomSheet = false },
            filterOptions = filterOptions,
            onSearchProviderClick = onSearchProviderClick,
            onShowZeroSeedersResultsClick = onShowZeroSeedersResultsClick,
        )
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
                    query = filterOptions.query,
                    onQueryChange = onFilterQueryChange,
                    placeholder = { Text(text = stringResource(R.string.search_results)) },
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

                    SortIconButton(onClick = { showSortMenu = true })
                    SortDropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        currentSortCriteria = currentSortCriteria,
                        currentSortOrder = currentSortOrder,
                        onSortRequest = onSortRequest,
                    )
                }

                IconButton(onClick = { showFilterOptionBottomSheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_filter_list),
                        contentDescription = null,
                    )
                }
            }
            SettingsIconButton(onClick = onNavigateToSettings)
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterOptionsBottomSheet(
    onDismissRequest: () -> Unit,
    filterOptions: FilterOptionsUiState,
    onSearchProviderClick: (SearchProviderId) -> Unit,
    onShowZeroSeedersResultsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(modifier = modifier, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(bottom = MaterialTheme.spaces.large)) {
            FiltersSectionTitle(titleId = R.string.filters_section_search_providers)
            SearchProvidersChipsRow(
                searchProviders = filterOptions.searchProviders,
                onSearchProviderClick = onSearchProviderClick,
                contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
            )

            FiltersSectionTitle(titleId = R.string.filters_section_additional_options)
            FlowRow(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = filterOptions.showZeroSeedersResults,
                    onClick = onShowZeroSeedersResultsClick,
                    label = {
                        Text(text = stringResource(R.string.filter_show_zero_seeders_results))
                    },
                )
            }
        }
    }
}

@Composable
private fun FiltersSectionTitle(@StringRes titleId: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.spaces.large,
                vertical = MaterialTheme.spaces.small,
            )
            .then(modifier),
        text = stringResource(titleId),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SearchProvidersChipsRow(
    searchProviders: List<SearchProviderFilterUiState>,
    onSearchProviderClick: (SearchProviderId) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val searchProviders = remember(searchProviders) {
        searchProviders.sortedBy { it.searchProviderName }
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = MaterialTheme.spaces.small),
        contentPadding = contentPadding,
    ) {
        items(items = searchProviders, key = { it.searchProviderId }) {
            FilterChip(
                selected = it.selected,
                onClick = { onSearchProviderClick(it.searchProviderId) },
                label = { Text(text = it.searchProviderName) },
                enabled = it.enabled,
            )
        }
    }
}

@Composable
private fun SearchResultsScreenContent(
    searchQuery: String,
    searchResults: List<Torrent>,
    filteredResults: List<Torrent>?,
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

            isInternetError && searchResults.isEmpty() -> NoInternetConnection(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )

            resultsNotFound -> ResultsNotFound(modifier = Modifier.fillMaxSize())

            filteredResults != null || searchResults.isNotEmpty() -> {
                TorrentList(
                    torrents = filteredResults ?: searchResults,
                    onTorrentSelect = onResultSelect,
                    toolbarContent = {
                        Text(
                            text = stringResource(
                                R.string.hint_results_count,
                                filteredResults?.size ?: searchResults.size,
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