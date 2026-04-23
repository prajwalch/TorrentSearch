package com.prajwalch.torrentsearch.ui.search

import androidx.compose.animation.AnimatedVisibility
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
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.extension.copyText
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
import com.prajwalch.torrentsearch.ui.rememberTorrentListState
import com.prajwalch.torrentsearch.ui.search.component.ResultsNotFoundState
import com.prajwalch.torrentsearch.ui.search.component.SearchFailuresBottomSheet
import com.prajwalch.torrentsearch.ui.search.component.SearchResults
import com.prajwalch.torrentsearch.ui.search.component.SearchResultsFilter

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
                viewModel.bookmarkTorrent(torrent = torrent)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = torrentBookmarkedMessage)
                }
            },
            onDownloadTorrent = {
                onDownloadTorrent(torrent.magnetUri())
            },
            onDownloadTorrentFile = {
                val torrentFileName = torrent.name.replace(' ', '-')

                if (torrent.fileDownloadLink != null) {
                    viewModel.downloadTorrentFile(
                        url = torrent.fileDownloadLink,
                        fileName = torrentFileName,
                    )
                } else {
                    viewModel.downloadTorrentFileUsingInfoHash(
                        infoHash = torrent.infoHash,
                        fileName = torrentFileName,
                    )
                }
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
                onOpenDescriptionPage(torrent.descriptionPageUrl, torrent.providerName)
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
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onFilterQueryChange = viewModel::filterSearchResultsByName,
                onChangeSortCriteria = viewModel::updateSortCriteria,
                onChangeSortOrder = viewModel::updateSortOrder,
                onShowSearchFailures = { showSearchFailures = true },
                onNavigateToSettings = onNavigateToSettings,
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
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.isInternetError && uiState.searchResults.successes.isEmpty() -> {
                NoInternetConnectionState(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    onTryAgain = viewModel::search,
                )
            }

            uiState.resultsNotFound -> {
                ResultsNotFoundState(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    onTryAgain = viewModel::search,
                )
            }

            /*
            FIXME: If we handle this, the filters (chips) will not be visible.
            uiState.resultsFilteredOut -> {
                ResultsNotFound(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }
            */

            else -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxWidth(),
                        visible = uiState.isSearching,
                    ) {
                        LinearProgressIndicator()
                    }

                    SearchResultsFilter(
                        filterOptions = uiState.filterOptions,
                        onToggleDeadTorrents = viewModel::toggleDeadTorrents,
                        onToggleHideViewed = viewModel::toggleHideViewedTorrents,
                        onToggleSearchProvider = viewModel::toggleSearchProviderResults,
                        onSelectAllSearchProviders = viewModel::selectAllSearchProviders,
                        onDeselectAllSearchProviders = viewModel::deselectAllSearchProviders,
                        onInvertSearchProvidersSelection = viewModel::invertSearchProvidersSelection,
                        onUpdateCategory = viewModel::updateCategoryFilter,
                        enableSearchProvidersFilter = !uiState.isSearching &&
                                uiState.filterOptions.searchProviders.isNotEmpty(),
                        // Enable only when there is a chance of receiving mixed category results,
                        // which is always the case when using `All`.
                        enableCategoryFilter = uiState.searchCategory == Category.All,
                    )
                    SearchResults(
                        searchResults = uiState.searchResults.successes,
                        onResultClick = {
                            selectedResult = it
                            viewModel.markAsViewed(it.infoHash)
                        },
                        searchQuery = uiState.searchQuery,
                        searchCategory = uiState.searchCategory,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshSearchResults,
                        viewedTorrentHashes = uiState.viewedTorrentHashes,
                        lazyListState = torrentListState.lazyListState,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenTopBar(
    uiState: SearchUiState,
    onNavigateBack: () -> Unit,
    onFilterQueryChange: (String) -> Unit,
    onChangeSortCriteria: (SortCriteria) -> Unit,
    onChangeSortOrder: (SortOrder) -> Unit,
    onShowSearchFailures: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val searchBarState = rememberCollapsibleSearchBarState(visibleOnInitial = false)
    var showSortOptions by rememberSaveable(uiState.sortOptions) { mutableStateOf(false) }

    val topBarActions: @Composable RowScope.() -> Unit = @Composable {
        val enableSearchResultsActions = when {
            uiState.isInternetError -> false
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
                onChangeCriteria = onChangeSortCriteria,
                currentOrder = uiState.sortOptions.order,
                onChangeOrder = onChangeSortOrder,
            )
        }

        // More menu.
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
                onShowSearchFailures = onShowSearchFailures,
                onNavigateToSettings = onNavigateToSettings,
                enableSearchFailuresAction = uiState.searchResults.failures.isNotEmpty(),
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
                placeholder = {
                    Text(text = stringResource(R.string.search_filter_query_hint))
                },
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
    onShowSearchFailures: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    enableSearchFailuresAction: Boolean = true,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.search_action_view_errors)) },
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
            text = { Text(text = stringResource(R.string.search_action_settings)) },
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