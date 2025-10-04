package com.prajwalch.torrentsearch.ui.searchresults

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SearchBar
import com.prajwalch.torrentsearch.ui.components.SearchIconButton
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
    onResultClick: (Torrent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SearchResultsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val searchBarFocusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showSearchBar by remember { mutableStateOf(false) }
    var showSortMenu by remember(uiState.currentSortCriteria, uiState.currentSortOrder) {
        mutableStateOf(false)
    }
    var showFilterOptions by remember { mutableStateOf(false) }
    val showScrollToTopButton by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 1 }
    }

    if (showFilterOptions) {
        FilterOptionsBottomSheet(
            onDismissRequest = { showFilterOptions = false },
            filterOptions = uiState.filterOptions,
            onToggleSearchProvider = viewModel::toggleSearchProviderResults,
            onToggleDeadTorrents = viewModel::toggleDeadTorrents,
        )
    }

    val topBarTitle: @Composable () -> Unit = @Composable {
        if (showSearchBar) {
            SearchBar(
                modifier = Modifier.focusRequester(searchBarFocusRequester),
                query = uiState.filterQuery,
                onQueryChange = viewModel::updateFilterQuery,
                placeholder = { Text(text = stringResource(R.string.search_results)) },
            )
        }
    }
    val topBarActions: @Composable RowScope.() -> Unit = @Composable {
        if (!showSearchBar) {
            SearchIconButton(onClick = { showSearchBar = true })
            SortIconButton(onClick = { showSortMenu = true })
            SortDropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                currentSortCriteria = uiState.currentSortCriteria,
                currentSortOrder = uiState.currentSortOrder,
                onSortRequest = viewModel::sortSearchResults,
            )
            IconButton(onClick = { showFilterOptions = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter_alt),
                    contentDescription = null,
                )
            }
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    ArrowBackIconButton(onClick = {
                        if (showSearchBar) showSearchBar = false else onNavigateBack()
                    })
                },
                title = topBarTitle,
                actions = topBarActions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ScrollToTopFAB(
                visible = showScrollToTopButton,
                onClick = { coroutineScope.launch { lazyListState.animateScrollToItem(0) } },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.isInternetError && uiState.searchResults.isEmpty() -> {
                NoInternetConnection(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onTryAgain = viewModel::performSearch,
                )
            }

            uiState.resultsNotFound -> {
                ResultsNotFound(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.filteredSearchResults != null || uiState.searchResults.isNotEmpty() -> {
                SearchResults(
                    modifier = Modifier.padding(innerPadding),
                    searchResults = uiState.filteredSearchResults ?: uiState.searchResults,
                    onResultClick = onResultClick,
                    searchQuery = uiState.searchQuery,
                    searchCategory = uiState.searchCategory,
                    isSearching = uiState.isSearching,
                    lazyListState = lazyListState,
                )
            }
        }
    }
}

@Composable
private fun NoInternetConnection(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        icon = R.drawable.ic_signal_wifi_off,
        title = R.string.msg_no_internet_connection,
        actions = {
            Button(onClick = onTryAgain) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.desc_retry_connection),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.button_try_again))
            }
        }
    )
}

@Composable
private fun ResultsNotFound(modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        icon = R.drawable.ic_results_not_found,
        title = R.string.msg_no_results_found,
    )
}

@Composable
private fun SearchResults(
    searchResults: List<Torrent>,
    onResultClick: (Torrent) -> Unit,
    searchQuery: String,
    searchCategory: Category,
    isSearching: Boolean,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = isSearching,
        ) {
            LinearProgressIndicator()
        }

        TorrentList(
            torrents = searchResults,
            onTorrentClick = onResultClick,
            headerContent = {
                Text(
                    text = stringResource(
                        R.string.hint_results_count,
                        searchResults.size,
                        searchQuery,
                        searchCategory,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            lazyListState = lazyListState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterOptionsBottomSheet(
    onDismissRequest: () -> Unit,
    filterOptions: FilterOptionsUiState,
    onToggleSearchProvider: (SearchProviderId) -> Unit,
    onToggleDeadTorrents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(modifier = modifier, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(bottom = MaterialTheme.spaces.large)) {
            FiltersSectionTitle(titleId = R.string.filters_section_search_providers)
            SearchProvidersChipsRow(
                searchProviders = filterOptions.searchProviders,
                onToggleSearchProvider = onToggleSearchProvider,
                contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
            )

            FiltersSectionTitle(titleId = R.string.filters_section_additional_options)
            FlowRow(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = filterOptions.deadTorrents,
                    onClick = onToggleDeadTorrents,
                    label = { Text(text = stringResource(R.string.filter_dead_torrents)) },
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
    onToggleSearchProvider: (SearchProviderId) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    // TODO: Sort this on ViewModel.
    val searchProviders = remember(searchProviders) {
        searchProviders.sortedBy { it.searchProviderName }
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
        ),
        contentPadding = contentPadding,
    ) {
        items(items = searchProviders, key = { it.searchProviderId }) {
            FilterChip(
                selected = it.selected,
                onClick = { onToggleSearchProvider(it.searchProviderId) },
                label = { Text(text = it.searchProviderName) },
                enabled = it.enabled,
            )
        }
    }
}