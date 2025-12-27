package com.prajwalch.torrentsearch.ui.searchhistory

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.extensions.copyText
import com.prajwalch.torrentsearch.domain.models.SearchHistory
import com.prajwalch.torrentsearch.domain.models.SearchHistoryId
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.DeleteForeverIconButton
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.SearchHistoryListItem

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
            EmptyPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                title = R.string.search_history_empty_message,
            )
        } else {
            SearchHistoryList(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                histories = searchHistoryList,
                onPerformSearch = onPerformSearch,
                onCopyQueryToClipboard = {
                    coroutineScope.launch {
                        clipboard.copyText(text = it)
                        snackbarHostState.showSnackbar(message = queryCopiedMessage)
                    }
                },
                onDelete = { viewModel.deleteSearchHistory(id = it) },
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun SearchHistoryList(
    histories: List<SearchHistory>,
    onPerformSearch: (String) -> Unit,
    onCopyQueryToClipboard: (String) -> Unit,
    onDelete: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    SearchHistoryList(
        modifier = modifier,
        histories = histories,
        historyListItem = { searchHistory ->
            SearchHistoryListItem(
                modifier = Modifier.animateItem(),
                searchHistory = searchHistory,
                onPerformSearch = onPerformSearch,
                onCopyQueryToClipboard = onCopyQueryToClipboard,
                onDelete = onDelete,
            )
        },
        key = { it.id },
        contentPadding = contentPadding,
    )
}

@Composable
private fun SearchHistoryListItem(
    searchHistory: SearchHistory,
    onPerformSearch: (String) -> Unit,
    onCopyQueryToClipboard: (String) -> Unit,
    onDelete: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchHistoryListItem(
        modifier = Modifier
            .combinedClickable(
                interactionSource = null,
                indication = LocalIndication.current,
                onClick = { onPerformSearch(searchHistory.query) },
                onLongClick = { onCopyQueryToClipboard(searchHistory.query) },
            )
            .then(modifier),
        query = searchHistory.query,
        onDeleteClick = { onDelete(searchHistory.id) },
    )
}

@Composable
private fun DeleteAllConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete_forever),
                contentDescription = null,
            )
        },
        title = {
            Text(text = stringResource(R.string.search_history_dialog_title_clear_history))
        },
        text = {
            Text(text = stringResource(R.string.search_history_dialog_message_clear_history))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.search_history_button_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.button_cancel))
            }
        },
    )
}