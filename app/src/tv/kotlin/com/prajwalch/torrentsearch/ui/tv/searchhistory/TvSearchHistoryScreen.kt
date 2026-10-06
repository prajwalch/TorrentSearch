package com.prajwalch.torrentsearch.ui.tv.searchhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.SearchHistoryDate
import com.prajwalch.torrentsearch.domain.model.SearchHistory
import com.prajwalch.torrentsearch.ui.searchhistory.SearchHistoryViewModel
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvFocusDefaults
import com.prajwalch.torrentsearch.ui.tv.component.TvMenuItem
import com.prajwalch.torrentsearch.ui.tv.component.TvMessageState
import com.prajwalch.torrentsearch.ui.tv.component.TvSectionHeader
import com.prajwalch.torrentsearch.ui.tv.component.TvTopBar
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding
import com.prajwalch.torrentsearch.ui.tv.settings.TvConfirmDialog

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.ImmutableMap

import org.koin.androidx.compose.koinViewModel

/**
 * TV search history.
 *
 * OK re-runs the search. There is deliberately no "copy query" row: the handheld
 * screen exposes it only via long-press, which a D-pad cannot reach, and copying to
 * the clipboard is not a useful action on a TV.
 */
@Composable
fun TvSearchHistoryScreen(
    onPerformSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchHistoryViewModel = koinViewModel(),
) {
    val histories: ImmutableMap<SearchHistoryDate, ImmutableList<SearchHistory>> by
        viewModel.uiState.collectAsStateWithLifecycle(initialValue = persistentMapOf())

    var pendingDelete by remember { mutableStateOf<SearchHistory?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(
            title = stringResource(R.string.search_history_screen_title),
            actions = {
                TvActionButton(onClick = { showClearAllConfirm = true }) {
                    Text(text = stringResource(R.string.search_history_action_delete_all))
                }
            },
        )

        if (histories.isEmpty()) {
            TvMessageState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.search_history_empty_message),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = tvListContentPadding(),
                verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve / 2),
            ) {
                histories.forEach { (date, entries) ->
                    item(key = "header-$date") {
                        TvSectionHeader(text = date.headerLabel())
                    }
                    items(entries, key = { "q-${it.id}" }) { history ->
                        TvMenuItem(
                            label = history.query,
                            onClick = { onPerformSearch(history.query) },
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { history ->
        TvConfirmDialog(
            title = stringResource(R.string.search_history_dialog_title_clear_history),
            message = stringResource(R.string.search_history_dialog_message_clear_history),
            confirmLabel = stringResource(R.string.search_history_button_clear),
            onConfirm = {
                viewModel.deleteSearchHistory(history.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showClearAllConfirm) {
        TvConfirmDialog(
            title = stringResource(R.string.search_history_dialog_title_clear_history),
            message = stringResource(R.string.search_history_dialog_message_clear_history),
            confirmLabel = stringResource(R.string.search_history_button_clear),
            onConfirm = {
                viewModel.deleteAllSearchHistory()
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false },
        )
    }
}

/** Today / Yesterday / a date, matching the handheld grouping. */
@Composable
private fun SearchHistoryDate.headerLabel(): String {
    val today = java.time.LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.search_history_today)
        today.minusDays(1) -> stringResource(R.string.search_history_yesterday)
        else -> date.toString()
    }
}