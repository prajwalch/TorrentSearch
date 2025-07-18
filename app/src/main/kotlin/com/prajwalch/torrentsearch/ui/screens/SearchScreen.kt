package com.prajwalch.torrentsearch.ui.screens

import android.content.ClipData

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.TopSearchBar
import com.prajwalch.torrentsearch.ui.components.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SortKey
import com.prajwalch.torrentsearch.ui.viewmodel.SortOrder

import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onDownloadTorrent: (MagnetUri) -> Unit,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val lazyListState = rememberLazyListState()
    val showScrollToUpButton by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 1 }
    }

    val clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLoading) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    selectedTorrent?.let { torrent ->
        val magnetLinkCopiedHint = stringResource(R.string.hint_magnet_link_copied)
        val descriptionPageUrlCopiedHint = stringResource(R.string.hint_description_page_url_copied)
        val hasDescriptionPage = torrent.descriptionPageUrl.isNotEmpty()

        TorrentActionsBottomSheet(
            title = torrent.name,
            isNSFW = torrent.category?.isNSFW ?: true,
            onDismissRequest = { selectedTorrent = null },
            onDownloadTorrent = { onDownloadTorrent(torrent.magnetUri()) },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.magnetUri())
                    snackbarHostState.showSnackbar(magnetLinkCopiedHint)
                }
            },
            onShareMagnetLink = { onShareMagnetLink(torrent.magnetUri()) },
            onOpenDescriptionPage = { onOpenDescriptionPage(torrent.descriptionPageUrl) },
            onCopyDescriptionPageUrl = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.descriptionPageUrl)
                    snackbarHostState.showSnackbar(descriptionPageUrlCopiedHint)
                }
            },
            onShareDescriptionPageUrl = { onShareDescriptionPageUrl(torrent.descriptionPageUrl) },
            hasDescriptionPage = hasDescriptionPage,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SearchScreenTopBar(
                modifier = Modifier.fillMaxWidth(),
                query = uiState.query,
                onQueryChange = viewModel::setQuery,
                onSearch = viewModel::performSearch,
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategoryChange = viewModel::setCategory,
                histories = uiState.histories,
                onDeleteSearchHistory = viewModel::deleteSearchHistory,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
        floatingActionButton = {
            ScrollToTopFAB(
                visible = showScrollToUpButton,
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
            onResultSelect = { selectedTorrent = it },
            lazyListState = lazyListState,
            isLoading = uiState.isLoading,
            isInternetError = uiState.isInternetError,
            onRetry = viewModel::performSearch,
            currentSortKey = uiState.currentSortKey,
            currentSortOrder = uiState.currentSortOrder,
            onSortResults = viewModel::sort,
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
                expanded = false
                onSearch()
            },
            expanded = expanded,
            onExpandChange = { expanded = it },
            onNavigateToSettings = onNavigateToSettings,
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
private fun ScrollToTopFAB(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(modifier = modifier, visible = visible) {
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

@Composable
private fun NoInternetConnectionMessage(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.msg_no_internet_connection), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.button_retry)) }
    }
}

@Composable
private fun ResultsNotFoundMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(58.dp),
            painter = painterResource(R.drawable.ic_sad),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.msg_no_results_found),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptySearchPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.msg_page_empty),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.msg_start_searching),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Copies the text into the clipboard. */
private suspend fun Clipboard.copyText(text: String) {
    val clipData = ClipData.newPlainText(
        /* label = */
        null,
        /* text = */
        text,
    )
    val clipEntry = ClipEntry(clipData = clipData)

    this@copyText.setClipEntry(clipEntry = clipEntry)
}