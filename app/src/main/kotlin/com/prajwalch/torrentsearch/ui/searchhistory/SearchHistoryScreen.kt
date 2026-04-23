package com.prajwalch.torrentsearch.ui.searchhistory

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.extension.copyText
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.DeleteForeverIconButton
import com.prajwalch.torrentsearch.ui.searchhistory.component.DeleteAllConfirmationDialog
import com.prajwalch.torrentsearch.ui.searchhistory.component.SearchHistoryList

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(
    onNavigateBack: () -> Unit,
    onPerformSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchHistoryViewModel = hiltViewModel(),
) {
    val searchHistoryList by viewModel.uiState.collectAsStateWithLifecycle()

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
            TopAppBar(
                navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
                title = { Text(text = stringResource(R.string.search_history_screen_title)) },
                actions = {
                    if (searchHistoryList.isNotEmpty()) {
                        DeleteForeverIconButton(
                            onClick = { showDeleteAllConfirmationDialog = true },
                            contentDescription = R.string.search_history_action_delete_all,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (searchHistoryList.isEmpty()) {
            ContentState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                title = { Text(stringResource(R.string.search_history_empty_message)) },
            )
        } else {
            SearchHistoryList(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                histories = searchHistoryList,
                onSearchRequest = onPerformSearch,
                onCopyQueryToClipboard = {
                    coroutineScope.launch {
                        clipboard.copyText(text = it)
                        snackbarHostState.showSnackbar(message = queryCopiedMessage)
                    }
                },
                onDeleteSearchHistory = { viewModel.deleteSearchHistory(id = it) },
                contentPadding = innerPadding,
            )
        }
    }
}