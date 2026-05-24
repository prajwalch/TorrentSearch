package com.prajwalch.torrentsearch.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.TorrentFileDownloadEffect
import com.prajwalch.torrentsearch.ui.component.AnimatedScrollToTopFAB
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.component.CollapsibleSearchBar
import com.prajwalch.torrentsearch.ui.component.NoInternetConnectionState
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.component.SearchIconButton
import com.prajwalch.torrentsearch.ui.component.SortDropdownMenu
import com.prajwalch.torrentsearch.ui.component.SortIconButton
import com.prajwalch.torrentsearch.ui.component.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.component.rememberCollapsibleSearchBarState
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.rememberTorrentListState
import com.prajwalch.torrentsearch.ui.search.component.ResultsNotFoundState
import com.prajwalch.torrentsearch.ui.search.component.SearchFailuresBottomSheet
import com.prajwalch.torrentsearch.ui.search.component.SearchResults
import com.prajwalch.torrentsearch.ui.search.component.TorrentFilter
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDownloadTorrent: (MagnetUri) -> Unit,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (url: String, providerName: String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torrentFileDownloadState by viewModel.torrentFileDownloadState.collectAsStateWithLifecycle()

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
            showNSFWBadge = torrent.isNSFW,
            onBookmarkTorrent = {
                viewModel.bookmarkTorrent(torrent)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(torrentBookmarkedMessage)
                }
            },
            onDownloadTorrent = { onDownloadTorrent(torrent.magnetUri()) },
            onDownloadTorrentFile = {
                if (torrent.fileDownloadLink != null) {
                    viewModel.downloadTorrentFile(
                        url = torrent.fileDownloadLink,
                        fileName = torrent.name,
                    )
                } else {
                    viewModel.downloadTorrentFileUsingInfoHash(
                        infoHash = torrent.infoHash,
                        fileName = torrent.name,
                    )
                }
            },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(torrent.magnetUri())
                    snackbarHostState.showSnackbar(magnetLinkCopiedMessage)
                }
            },
            onShareMagnetLink = { onShareMagnetLink(torrent.magnetUri()) },
            onOpenDescriptionPage = {
                onOpenDescriptionPage(torrent.descriptionPageUrl, torrent.providerName)
            },
            onCopyDescriptionPageUrl = {
                coroutineScope.launch {
                    clipboard.copyText(torrent.descriptionPageUrl)
                    snackbarHostState.showSnackbar(urlCopiedMessage)
                }
            },
            onShareDescriptionPageUrl = { onShareDescriptionPageUrl(torrent.descriptionPageUrl) },
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
        topBar = {
            SearchScreenTopBar(
                onNavigateBack = onNavigateBack,
                onFilterQueryChange = viewModel::filterSearchResultsByName,
                sortOptions = uiState.sortOptions,
                onChangeSortCriteria = viewModel::updateSortCriteria,
                onChangeSortOrder = viewModel::updateSortOrder,
                onRefresh = viewModel::refreshSearchResults,
                onStopSearch = viewModel::stopSearch,
                onShowSearchFailures = { showSearchFailures = true },
                onNavigateToSettings = onNavigateToSettings,
                searchState = uiState.searchState,
                enableSearchResultsAction = uiState.searchState is SearchState.ResultsAvailable,
                enableSearchFailuresAction = uiState.searchResults.failures.isNotEmpty(),
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
        AnimatedContent(
            modifier = Modifier.padding(innerPadding),
            targetState = uiState.searchState,
            contentKey = { it.getAnimationContentKey() },
        ) { searchState ->
            when (searchState) {
                SearchState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                SearchState.InternetError -> {
                    NoInternetConnectionState(
                        modifier = Modifier.fillMaxSize(),
                        onTryAgain = viewModel::search,
                    )
                }

                SearchState.ResultsNotFound -> {
                    ResultsNotFoundState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.spaces.large),
                        onTryAgain = viewModel::search,
                        query = uiState.searchParams.query,
                        category = uiState.searchParams.category,
                    )
                }

                is SearchState.ResultsAvailable -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            modifier = Modifier.fillMaxWidth(),
                            visible = searchState is SearchState.ResultsAvailable.Searching,
                        ) {
                            LinearProgressIndicator()
                        }

                        TorrentFilter(
                            filter = uiState.torrentFilter,
                            onToggleDeadTorrents = viewModel::toggleDeadTorrents,
                            onToggleHideViewed = viewModel::toggleHideViewedTorrents,
                            onToggleSearchProvider = viewModel::toggleSearchProviderResults,
                            onSelectAllSearchProviders = viewModel::selectAllSearchProviders,
                            onDeselectAllSearchProviders = viewModel::deselectAllSearchProviders,
                            onInvertSearchProvidersSelection = viewModel::invertSearchProvidersSelection,
                            onUpdateCategory = viewModel::updateCategoryFilter,
                            enableSearchProvidersFilter =
                                searchState is SearchState.ResultsAvailable.Complete &&
                                        uiState.torrentFilter.providers.isNotEmpty(),
                            // Enable only when there is a chance of receiving mixed category results,
                            // which is always the case when using `All`.
                            enableCategoryFilter = uiState.searchParams.category == Category.All,
                        )
                        SearchResults(
                            searchResults = uiState.searchResults.successes,
                            onResultClick = {
                                selectedResult = it
                                viewModel.markAsViewed(it.infoHash)
                            },
                            searchQuery = uiState.searchParams.query,
                            searchCategory = uiState.searchParams.category,
                            isRefreshing = searchState is SearchState.ResultsAvailable.Refreshing,
                            onRefresh = viewModel::refreshSearchResults,
                            viewedTorrentHashes = uiState.viewedTorrentHashes,
                            lazyListState = torrentListState.lazyListState,
                        )
                    }
                }
            }
        }
    }
}

private fun SearchState.getAnimationContentKey() = when (this) {
    SearchState.InternetError -> SearchState.InternetError::class
    SearchState.Loading -> SearchState.Loading::class
    SearchState.ResultsNotFound -> SearchState.ResultsNotFound::class
    is SearchState.ResultsAvailable -> SearchState.ResultsAvailable::class
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenTopBar(
    onNavigateBack: () -> Unit,
    onFilterQueryChange: (String) -> Unit,
    sortOptions: SortOptions,
    onChangeSortCriteria: (SortCriteria) -> Unit,
    onChangeSortOrder: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onStopSearch: () -> Unit,
    onShowSearchFailures: () -> Unit,
    onNavigateToSettings: () -> Unit,
    searchState: SearchState,
    modifier: Modifier = Modifier,
    enableSearchResultsAction: Boolean = true,
    enableSearchFailuresAction: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val searchBarState = rememberCollapsibleSearchBarState(visibleOnInitial = false)
    var showSortOptions by rememberSaveable(sortOptions) { mutableStateOf(false) }

    val topBarActions: @Composable RowScope.() -> Unit = @Composable {
        if (!searchBarState.isVisible) {
            SearchIconButton(
                onClick = { searchBarState.showSearchBar() },
                enabled = enableSearchResultsAction,
            )
            SortIconButton(
                onClick = { showSortOptions = true },
                enabled = enableSearchResultsAction,
            )
            SortDropdownMenu(
                expanded = showSortOptions,
                onDismissRequest = { showSortOptions = false },
                currentCriteria = sortOptions.criteria,
                onChangeCriteria = onChangeSortCriteria,
                currentOrder = sortOptions.order,
                onChangeOrder = onChangeSortOrder,
            )
        }

        Box {
            var showMoreActions by rememberSaveable { mutableStateOf(false) }

            IconButton(onClick = { showMoreActions = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = null,
                )
            }
            TopBarMoreMenu(
                expanded = showMoreActions,
                onDismiss = { showMoreActions = false },
                onRefresh = onRefresh,
                onStopSearch = onStopSearch,
                onShowSearchFailures = onShowSearchFailures,
                onNavigateToSettings = onNavigateToSettings,
                searchState = searchState,
                enableSearchFailuresAction = enableSearchFailuresAction,
            )
        }
    }

    TopAppBar(
        modifier = modifier,
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        title = {
            CollapsibleSearchBar(
                state = searchBarState,
                onQueryChange = onFilterQueryChange,
                placeholder = { Text(stringResource(R.string.search_filter_query_hint)) },
            )
        },
        actions = topBarActions,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TopBarMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onStopSearch: () -> Unit,
    onShowSearchFailures: () -> Unit,
    onNavigateToSettings: () -> Unit,
    searchState: SearchState,
    modifier: Modifier = Modifier,
    enableSearchFailuresAction: Boolean = true,
) {
    val refreshAction: @Composable (enable: Boolean) -> Unit = @Composable { enable ->
        DropdownMenuItem(
            onClick = {
                onRefresh()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                )
            },
            text = { Text(stringResource(R.string.search_action_refresh)) },
            enabled = enable,
        )
    }
    val stopSearchAction: @Composable (enable: Boolean) -> Unit = @Composable { enable ->
        DropdownMenuItem(
            onClick = {
                onStopSearch()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = null,
                )
            },
            text = { Text(stringResource(R.string.search_action_stop_search)) },
            enabled = enable,
        )
    }
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        Crossfade(searchState) { innerSearchState ->
            when (innerSearchState) {
                SearchState.Loading -> stopSearchAction(false)
                SearchState.InternetError -> refreshAction(false)
                SearchState.ResultsNotFound -> refreshAction(false)
                SearchState.ResultsAvailable.Complete -> refreshAction(true)
                SearchState.ResultsAvailable.Refreshing -> refreshAction(false)
                SearchState.ResultsAvailable.Searching -> stopSearchAction(true)
            }
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.search_action_view_errors)) },
            onClick = {
                onShowSearchFailures()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                )
            },
            enabled = enableSearchFailuresAction,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.search_action_settings)) },
            onClick = {
                onNavigateToSettings()
                onDismiss()
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