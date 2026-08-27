package com.prajwalch.torrentsearch.ui.searchhistory

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.searchhistory.component.DeleteAllConfirmationDialog
import com.prajwalch.torrentsearch.ui.searchhistory.component.SearchHistoryList

import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(
    onNavigateBack: () -> Unit,
    onPerformSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchHistoryViewModel = koinViewModel(),
) {
    val searchHistoriesByDate by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val clipboard = LocalClipboard.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val queryCopiedMessage = stringResource(R.string.search_history_query_copied_message)

    var showDeleteAllConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteAllConfirmationDialog) {
        DeleteAllConfirmationDialog(
            onDismiss = { showDeleteAllConfirmationDialog = false },
            onConfirm = {
                viewModel.deleteAllSearchHistory()
                showDeleteAllConfirmationDialog = false
            },
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(connection = scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            SearchHistoryScreenTopBar(
                onNavigateBack = onNavigateBack,
                showDeleteAction = searchHistoriesByDate != null,
                onDeleteAllSearchHistory = { showDeleteAllConfirmationDialog = true },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val innerSearchHistoriesByDate = searchHistoriesByDate

        if (innerSearchHistoriesByDate == null) {
            ContentState(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                title = { Text(stringResource(R.string.search_history_empty_message)) },
            )
        } else {
            SearchHistoryList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .consumeWindowInsets(innerPadding),
                histories = innerSearchHistoriesByDate,
                onSearchRequest = onPerformSearch,
                onCopyQueryToClipboard = {
                    coroutineScope.launch {
                        clipboard.copyText(text = it)
                        snackbarHostState.showSnackbar(message = queryCopiedMessage)
                    }
                },
                onDeleteSearchHistory = { viewModel.deleteSearchHistory(id = it) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHistoryScreenTopBar(
    onNavigateBack: () -> Unit,
    showDeleteAction: Boolean,
    onDeleteAllSearchHistory: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        title = { Text(stringResource(R.string.search_history_screen_title)) },
        actions = {
            if (showDeleteAction) {
                DeleteSweepIconButton(
                    onClick = onDeleteAllSearchHistory,
                    contentDescription = stringResource(R.string.search_history_action_delete_all),
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun DeleteSweepIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete_sweep),
            contentDescription = contentDescription,
        )
    }
}