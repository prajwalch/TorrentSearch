package com.prajwalch.torrentsearch.ui.tv.settings.defaultsortoptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.ui.settings.defaultsortoptions.DefaultSortOptionsViewModel
import com.prajwalch.torrentsearch.ui.tv.component.TvFocusDefaults
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvMenuItem
import com.prajwalch.torrentsearch.ui.tv.component.TvSectionHeader
import com.prajwalch.torrentsearch.ui.tv.component.TvTopBar
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding

import org.koin.androidx.compose.koinViewModel

/**
 * TV default sort options.
 *
 * The handheld screen puts both option groups in a plain `Column`, which has no
 * lazy focus traversal - focus can walk past the end without scrolling. `LazyColumn`
 * is used here so the D-pad scrolls the options.
 *
 * Selection is the fill, focus is the ring, as everywhere else.
 */
@Composable
fun TvDefaultSortOptionsScreen(
    modifier: Modifier = Modifier,
    viewModel: DefaultSortOptionsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Screen entry point: focus lands on the first sort option when this route opens.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(title = stringResource(R.string.settings_default_sort_options))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = tvListContentPadding(),
            verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
        ) {
            item {
                TvSectionHeader(text = stringResource(R.string.settings_section_sort_criteria))
            }
            items(SortCriteria.entries.toList()) { criteria ->
                val isFirstItem = criteria == SortCriteria.entries.first()
                TvMenuItem(
                    modifier = if (isFirstItem && entryFocusRequester != null) {
                        Modifier.focusRequester(entryFocusRequester)
                    } else {
                        Modifier
                    },
                    label = stringResource(criteria.labelRes()),
                    active = criteria == uiState.criteria,
                    onClick = { viewModel.setDefaultSortCriteria(criteria) },
                )
            }

            item {
                TvSectionHeader(text = stringResource(R.string.settings_section_sort_order))
            }
            items(SortOrder.entries.toList()) { order ->
                TvMenuItem(
                    label = stringResource(order.labelRes()),
                    active = order == uiState.order,
                    onClick = { viewModel.setDefaultSortOrder(order) },
                )
            }
        }
    }
}

@Composable
private fun SortCriteria.labelRes(): Int = when (this) {
    SortCriteria.Name -> R.string.action_sort_criteria_name
    SortCriteria.Seeders -> R.string.action_sort_criteria_seeders
    SortCriteria.Peers -> R.string.action_sort_criteria_peers
    SortCriteria.FileSize -> R.string.action_sort_criteria_file_size
    SortCriteria.Date -> R.string.action_sort_criteria_date
}

@Composable
private fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.Ascending -> R.string.action_sort_order_ascending
    SortOrder.Descending -> R.string.action_sort_order_descending
}