package com.prajwalch.torrentsearch.ui.searchresults

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.SortCriteria
import com.prajwalch.torrentsearch.data.repository.SortOrder
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.NoInternetConnection
import com.prajwalch.torrentsearch.ui.components.ResultsNotFound
import com.prajwalch.torrentsearch.ui.components.ScrollToTopFAB
import com.prajwalch.torrentsearch.ui.components.SortMenu
import com.prajwalch.torrentsearch.ui.components.TorrentList

import kotlinx.coroutines.launch

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

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchResultsScreenTopBar(
                onNavigateBack = onNavigateBack,
                onNavigateToSettings = onNavigateToSettings,
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
                results = uiState.results,
                resultsNotFound = uiState.resultsNotFound,
                onResultSelect = onResultSelect,
                lazyListState = lazyListState,
                isLoading = uiState.isLoading,
                isInternetError = uiState.isInternetError,
                onRetry = { viewModel.performSearch() },
                currentSortCriteria = uiState.currentSortCriteria,
                currentSortOrder = uiState.currentSortOrder,
                onSortResults = viewModel::sortResults,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsScreenTopBar(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = { NavigateBackIconButton(onClick = onNavigateBack) },
        title = {},
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
            }
        }
    )
}

@Composable
private fun SearchResultsScreenContent(
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

            resultsNotFound -> ResultsNotFound(modifier = Modifier.fillMaxSize())

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