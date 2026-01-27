package com.prajwalch.torrentsearch.ui.search

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.MagnetUri
import com.prajwalch.torrentsearch.domain.models.SearchException
import com.prajwalch.torrentsearch.domain.models.Torrent
import com.prajwalch.torrentsearch.extensions.copyText
import com.prajwalch.torrentsearch.ui.components.AnimatedScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.BottomInfo
import com.prajwalch.torrentsearch.ui.components.CollapsibleSearchBar
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.LazyColumnWithScrollbar
import com.prajwalch.torrentsearch.ui.components.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.components.SearchIconButton
import com.prajwalch.torrentsearch.ui.components.SortDropdownMenu
import com.prajwalch.torrentsearch.ui.components.SortIconButton
import com.prajwalch.torrentsearch.ui.components.StackTraceCard
import com.prajwalch.torrentsearch.ui.components.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.components.TorrentListItem
import com.prajwalch.torrentsearch.ui.components.rememberCollapsibleSearchBarState
import com.prajwalch.torrentsearch.ui.rememberTorrentListState
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.utils.categoryStringResource

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDownloadTorrent: (MagnetUri) -> Unit,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val torrentListState = rememberTorrentListState(
        itemsCount = { uiState.searchResults.successes.size },
    )

    // Torrent actions state.
    var selectedResult by retain { mutableStateOf<Torrent?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    selectedResult?.let { torrent ->
        val clipboard = LocalClipboard.current

        val torrentBookmarkedMessage = stringResource(
            R.string.bookmarked_message,
        )
        val magnetLinkCopiedMessage = stringResource(
            R.string.torrent_list_magnet_link_copied_message
        )
        val urlCopiedMessage = stringResource(
            R.string.torrent_list_url_copied_message,
        )

        TorrentActionsBottomSheet(
            onDismiss = { selectedResult = null },
            title = torrent.name,
            showNSFWBadge = torrent.isNSFW(),
            onBookmarkTorrent = {
                viewModel.bookmarkTorrent(torrent = torrent)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = torrentBookmarkedMessage)
                }
            },
            onDownloadTorrent = {
                onDownloadTorrent(torrent.magnetUri())
            },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.magnetUri())
                    snackbarHostState.showSnackbar(message = magnetLinkCopiedMessage)
                }
            },
            onShareMagnetLink = {
                onShareMagnetLink(torrent.magnetUri())
            },
            onOpenDescriptionPage = {
                onOpenDescriptionPage(torrent.descriptionPageUrl)
            },
            onCopyDescriptionPageUrl = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.descriptionPageUrl)
                    snackbarHostState.showSnackbar(message = urlCopiedMessage)
                }
            },
            onShareDescriptionPageUrl = {
                onShareDescriptionPageUrl(torrent.descriptionPageUrl)
            },
            enableDescriptionPageActions = torrent.descriptionPageUrl.isNotEmpty(),
        )
    }

    var showSearchFailures by rememberSaveable { mutableStateOf(false) }
    if (showSearchFailures) {
        SearchFailuresBottomSheet(
            onDismiss = { showSearchFailures = false },
            failures = uiState.searchResults.failures,
        )
    }

    var showFilterOptions by rememberSaveable { mutableStateOf(false) }
    if (showFilterOptions) {
        FilterOptionsBottomSheet(
            onDismissRequest = { showFilterOptions = false },
            filterOptions = uiState.filterOptions,
            onToggleSearchProvider = viewModel::toggleSearchProviderResults,
            onToggleDeadTorrents = viewModel::toggleDeadTorrents,
            isSearching = uiState.isSearching,
        )
    }

    val searchBarState = rememberCollapsibleSearchBarState(visibleOnInitial = false)
    var showSortOptions by rememberSaveable(uiState.sortOptions) { mutableStateOf(false) }

    val topBarTitle: @Composable () -> Unit = @Composable {
        CollapsibleSearchBar(
            state = searchBarState,
            onQueryChange = viewModel::filterSearchResults,
            placeholder = { Text(text = stringResource(R.string.search_query_hint)) },
        )
    }
    val topBarActions: @Composable RowScope.() -> Unit = @Composable {
        val enableSearchResultsActions = when {
            uiState.resultsNotFound -> false
            uiState.resultsFilteredOut -> true
            else -> uiState.searchResults.successes.isNotEmpty()
        }

        if (!searchBarState.isVisible) {
            SearchIconButton(
                onClick = { searchBarState.showSearchBar() },
                enabled = enableSearchResultsActions,
            )
            SortIconButton(
                onClick = { showSortOptions = true },
                enabled = enableSearchResultsActions,
            )
            SortDropdownMenu(
                expanded = showSortOptions,
                onDismissRequest = { showSortOptions = false },
                currentCriteria = uiState.sortOptions.criteria,
                onChangeCriteria = viewModel::updateSortCriteria,
                currentOrder = uiState.sortOptions.order,
                onChangeOrder = viewModel::updateSortOrder,
            )
            IconButton(
                onClick = { showFilterOptions = true },
                enabled = enableSearchResultsActions,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter_alt),
                    contentDescription = stringResource(R.string.search_action_filter),
                )
            }
        }
        Box {
            var showAdditionalActionsMenu by rememberSaveable { mutableStateOf(false) }

            IconButton(onClick = { showAdditionalActionsMenu = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = null,
                )
            }
            RoundedDropdownMenu(
                expanded = showAdditionalActionsMenu,
                onDismissRequest = { showAdditionalActionsMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.search_action_view_errors)) },
                    onClick = {
                        showSearchFailures = true
                        showAdditionalActionsMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_error),
                            contentDescription = null,
                        )
                    },
                    enabled = uiState.searchResults.failures.isNotEmpty(),
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.search_action_settings)) },
                    onClick = {
                        onNavigateToSettings()
                        showAdditionalActionsMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            TopAppBar(
                navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
                title = topBarTitle,
                actions = topBarActions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedScrollToTopFAB(
                visible = torrentListState.showScrollTopButton,
                onClick = { coroutineScope.launch { torrentListState.scrollToTop() } },
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

            uiState.isInternetError && uiState.searchResults.successes.isEmpty() -> {
                NoInternetConnection(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onTryAgain = viewModel::reload,
                )
            }

            uiState.resultsNotFound -> {
                ResultsNotFound(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onTryAgain = viewModel::reload,
                )
            }

            uiState.resultsFilteredOut -> {
                ResultsNotFound(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                SearchResults(
                    modifier = Modifier.padding(innerPadding),
                    searchResults = uiState.searchResults.successes,
                    onResultClick = { selectedResult = it },
                    searchQuery = uiState.searchQuery,
                    searchCategory = uiState.searchCategory,
                    isSearching = uiState.isSearching,
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refreshSearchResults,
                    lazyListState = torrentListState.lazyListState,
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
        title = R.string.search_internet_connection_error,
        actions = { TryAgainButton(onClick = onTryAgain) }
    )
}

@Composable
private fun ResultsNotFound(modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        icon = R.drawable.ic_results_not_found,
        title = R.string.search_no_results_message,
    )
}

@Composable
private fun ResultsNotFound(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    EmptyPlaceholder(
        modifier = modifier,
        icon = R.drawable.ic_results_not_found,
        title = R.string.search_no_results_message,
        actions = { TryAgainButton(onClick = onTryAgain) }
    )
}

@Composable
private fun TryAgainButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(modifier = modifier, onClick = onClick) {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.search_button_try_again))
    }
}

@Composable
private fun SearchResults(
    searchResults: ImmutableList<Torrent>,
    onResultClick: (Torrent) -> Unit,
    searchQuery: String,
    searchCategory: Category,
    isSearching: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        Column {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = isSearching,
            ) {
                LinearProgressIndicator()
            }

            LazyColumnWithScrollbar(state = lazyListState) {
                item {
                    SearchResultsCount(
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spaces.large,
                            vertical = MaterialTheme.spaces.small,
                        ),
                        searchResultsSize = searchResults.size,
                        searchQuery = searchQuery,
                        searchCategory = searchCategory,
                    )
                }

                items(items = searchResults, contentType = { it.category }) {
                    TorrentListItem(
                        modifier = Modifier
                            .animateItem()
                            .clickable { onResultClick(it) },
                        name = it.name,
                        size = it.size,
                        seeders = it.seeders,
                        peers = it.peers,
                        uploadDate = it.uploadDate,
                        category = it.category,
                        providerName = it.providerName,
                        isNSFW = it.isNSFW(),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SearchResultsCount(
    searchResultsSize: Int,
    searchQuery: String,
    searchCategory: Category,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(
            R.string.search_results_count_format,
            searchResultsSize,
            searchQuery,
            categoryStringResource(searchCategory),
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterOptionsBottomSheet(
    onDismissRequest: () -> Unit,
    filterOptions: FilterOptions,
    onToggleSearchProvider: (String) -> Unit,
    onToggleDeadTorrents: () -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(modifier = modifier, onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(bottom = MaterialTheme.spaces.large)) {
            if (filterOptions.searchProviders.isNotEmpty()) {
                FiltersSectionTitle(titleId = R.string.search_filters_section_search_providers)
                SearchProvidersChipsRow(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                    filterOptions = filterOptions.searchProviders,
                    onToggleSearchProvider = onToggleSearchProvider,
                    isSearching = isSearching,
                )
            }

            FiltersSectionTitle(titleId = R.string.search_filters_section_additional_options)
            FlowRow(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = filterOptions.deadTorrents,
                    onClick = onToggleDeadTorrents,
                    label = { Text(text = stringResource(R.string.search_filters_dead_torrents)) },
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
    filterOptions: ImmutableList<SearchProviderFilterOption>,
    onToggleSearchProvider: (String) -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
        ),
    ) {
        for (filterOption in filterOptions) {
            FilterChip(
                selected = filterOption.selected,
                onClick = { onToggleSearchProvider(filterOption.searchProviderName) },
                label = { Text(text = filterOption.searchProviderName) },
                enabled = !isSearching,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFailuresBottomSheet(
    onDismiss: () -> Unit,
    failures: ImmutableList<SearchException>,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                text = stringResource(R.string.search_errors_bottom_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
            HorizontalDivider()
            SearchExceptionList(
                modifier = Modifier.weight(1f),
                exceptions = failures,
                contentPadding = PaddingValues(MaterialTheme.spaces.large),
            )
            HorizontalDivider()
            BottomInfo(modifier = Modifier.padding(MaterialTheme.spaces.large)) {
                Text(text = stringResource(R.string.search_info_troubleshoot_help))
            }
        }
    }
}

@Preview
@Composable
private fun SearchFailuresBottomSheetPreview() {
    val failures = persistentListOf(
        SearchException(
            searchProviderName = "ThePirateBay",
            searchProviderUrl = "https://example.com",
        ),
        SearchException(
            searchProviderName = "TheRarBg",
            searchProviderUrl = "https://example.com",
        ),
        SearchException(
            searchProviderName = "TorrentDownloads",
            searchProviderUrl = "https://example.com",
        ),
        SearchException(
            searchProviderName = "TokyoToshokan",
            searchProviderUrl = "https://example.com",
        ),
    )

    TorrentSearchTheme(darkTheme = true) {
        SearchFailuresBottomSheet(
            onDismiss = {},
            failures = failures,
        )
    }
}

@Composable
private fun SearchExceptionList(
    exceptions: ImmutableList<SearchException>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
        ),
        contentPadding = contentPadding,
    ) {
        items(items = exceptions, key = { it.searchProviderUrl }) {
            SearchExceptionListItem(
                modifier = Modifier.animateItem(),
                exception = it,
            )
        }
    }
}

@Composable
private fun SearchExceptionListItem(
    exception: SearchException,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
    ) {
        var showStackTrace by rememberSaveable { mutableStateOf(false) }

        val exceptionMessage = exception.message ?: stringResource(R.string.search_unexpected_error)
        val listItemColors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            supportingColor = MaterialTheme.colorScheme.error,
        )

        ListItem(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.medium)
                .clickable { showStackTrace = !showStackTrace },
            headlineContent = { Text(text = exception.searchProviderName) },
            supportingContent = { Text(text = exceptionMessage) },
            trailingContent = {
                AnimatedContent(targetState = showStackTrace) { stackTraceVisible ->
                    val iconId = if (stackTraceVisible) {
                        R.drawable.ic_keyboard_arrow_up
                    } else {
                        R.drawable.ic_keyboard_arrow_down
                    }
                    Icon(
                        painter = painterResource(iconId),
                        contentDescription = null,
                    )
                }
            },
            colors = listItemColors,
        )

        AnimatedVisibility(visible = showStackTrace) {
            StackTraceCard(
                modifier = Modifier.height(360.dp),
                stackTrace = exception.stackTraceToString(),
            )
        }
    }
}