package com.prajwalch.torrentsearch.ui.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
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
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.TorrentFileDownloadEffect
import com.prajwalch.torrentsearch.ui.browse.component.BrowseFilters
import com.prajwalch.torrentsearch.ui.browse.component.NoTorrentsFoundState
import com.prajwalch.torrentsearch.ui.browse.component.TorrentList
import com.prajwalch.torrentsearch.ui.component.AnimatedScrollToTopFAB
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.component.CollapsibleSearchBar
import com.prajwalch.torrentsearch.ui.component.NoInternetConnectionState
import com.prajwalch.torrentsearch.ui.component.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.component.rememberCollapsibleSearchBarState
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.rememberTorrentListState
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProvidersSettings: () -> Unit,
    onDownloadTorrent: (MagnetUri) -> Unit,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (url: String, providerName: String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torrentFileDownloadState by viewModel.torrentFileDownloadState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val torrentListState = rememberTorrentListState(itemsCount = { uiState.torrents.size })

    var selectedTorrent by retain { mutableStateOf<Torrent?>(null) }
    selectedTorrent?.let { torrent ->
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
            onDismiss = { selectedTorrent = null },
            title = torrent.name,
            showNSFWBadge = torrent.isNSFW,
            onBookmarkTorrent = {
                viewModel.bookmarkTorrent(torrent)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = torrentBookmarkedMessage)
                }
            },
            onDownloadTorrent = {
                onDownloadTorrent(torrent.magnetUri())
            },
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

    TorrentFileDownloadEffect(
        onWrite = viewModel::writeTorrentFile,
        state = torrentFileDownloadState,
        events = viewModel.torrentFileDownloadEvents,
        snackbarHostState = snackbarHostState,
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BrowseScreenTopBar(
                onNavigateBack = onNavigateBack,
                onSearchQueryChange = viewModel::searchTorrents,
                onNavigateToSettings = onNavigateToSettings,
                enableSearchAction = uiState.contentState is BrowseContentState.Available,
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = uiState.contentState is BrowseContentState.Available.Searching,
            ) {
                LinearProgressIndicator()
            }

            val enableViewFilters = uiState.contentState is BrowseContentState.Available
            BrowseFilters(
                sort = uiState.queryParams.sort,
                onChangeSort = viewModel::updateBrowseSort,
                category = uiState.queryParams.category,
                onChangeCategory = viewModel::updateCategory,
                deadTorrents = uiState.viewFilters.deadTorrents,
                onToggleDeadTorrents = viewModel::toggleDeadTorrents,
                hideViewed = uiState.viewFilters.hideViewed,
                onToggleHideViewed = viewModel::toggleHideViewed,
                providerOptions = uiState.viewFilters.providers,
                onToggleSearchProvider = viewModel::toggleSearchProviderResults,
                onSelectAllSearchProviders = viewModel::selectAllSearchProviders,
                onDeselectAllSearchProviders = viewModel::deselectAllSearchProviders,
                onInvertSearchProvidersSelection = viewModel::invertSearchProvidersSelection,
                enableDeadTorrents = enableViewFilters,
                enableHideViewed = enableViewFilters,
                enableSearchProvidersFilter = enableViewFilters,
                contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
            )

            AnimatedContent(
                targetState = uiState.contentState,
                contentKey = { it.getAnimationContentKey() },
            ) { contentState ->
                when (contentState) {
                    BrowseContentState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    BrowseContentState.InternetError -> {
                        NoInternetConnectionState(
                            modifier = Modifier.fillMaxSize(),
                            onTryAgain = viewModel::loadTorrents,
                        )
                    }

                    BrowseContentState.NotAvailable -> {
                        NoTorrentsFoundState(
                            modifier = Modifier.fillMaxSize(),
                            onNavigateToSearchProviders = onNavigateToProvidersSettings,
                            onRetry = viewModel::loadTorrents,
                        )
                    }

                    is BrowseContentState.Available -> {
                        TorrentList(
                            torrents = uiState.torrents,
                            onTorrentClick = {
                                selectedTorrent = it
                                viewModel.markAsViewed(it.infoHash)
                            },
                            isRefreshing = contentState is BrowseContentState.Available.Refreshing,
                            onRefresh = viewModel::refreshTorrents,
                            viewedTorrentHashes = uiState.viewedTorrentHashes,
                            lazyListState = torrentListState.lazyListState,
                        )
                    }
                }
            }
        }
    }
}

private fun BrowseContentState.getAnimationContentKey() = when (this) {
    BrowseContentState.InternetError -> BrowseContentState.InternetError::class
    BrowseContentState.Loading -> BrowseContentState.Loading::class
    BrowseContentState.NotAvailable -> BrowseContentState.NotAvailable::class
    is BrowseContentState.Available -> BrowseContentState.Available::class
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseScreenTopBar(
    onNavigateBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    enableSearchAction: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val searchBarState = rememberCollapsibleSearchBarState(visibleOnInitial = false)

    TopAppBar(
        modifier = modifier,
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        title = {
            CollapsibleSearchBar(
                state = searchBarState,
                onQueryChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.browse_search_query_hint)) },
            )

            if (!searchBarState.isVisible) {
                Text(stringResource(R.string.browse_screen_title))
            }
        },
        actions = {
            if (!searchBarState.isVisible) {
                IconButton(
                    onClick = { searchBarState.showSearchBar() },
                    enabled = enableSearchAction,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                    )
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}