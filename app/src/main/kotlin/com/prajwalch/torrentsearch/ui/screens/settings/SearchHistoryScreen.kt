package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.components.EmptyPlaceholder
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.SearchHistoryListItem
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryViewModel

@Composable
fun SearchHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchHistoryViewModel,
    modifier: Modifier = Modifier,
) {
    val searchHistoryList by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchHistoryScreenTopBar(
                onNavigateBack = onNavigateBack,
                onDeleteAll = viewModel::deleteAllSearchHistory,
                showDeleteAllAction = searchHistoryList.isNotEmpty(),
            )
        }
    ) { innerPadding ->
        if (searchHistoryList.isEmpty()) {
            EmptyPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                headlineId = R.string.msg_nothing_here_yet,
            )
        } else {
            SearchHistoryList(
                modifier = Modifier.consumeWindowInsets(innerPadding),
                histories = searchHistoryList,
                historyListItem = {
                    SearchHistoryListItem(
                        modifier = Modifier.animateItem(),
                        query = it.query,
                        onDeleteClick = { viewModel.deleteSearchHistory(id = it.id) },
                    )
                },
                contentPadding = innerPadding,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHistoryScreenTopBar(
    onNavigateBack: () -> Unit,
    onDeleteAll: () -> Unit,
    showDeleteAllAction: Boolean,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_to_settings_screen,
            )
        },
        title = { Text(text = stringResource(R.string.search_history_screen_title)) },
        actions = {
            AnimatedVisibility(visible = showDeleteAllAction) {
                IconButton(onClick = onDeleteAll) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_history),
                        contentDescription = stringResource(
                            R.string.desc_delete_all_search_history
                        ),
                    )
                }
            }
        }
    )
}