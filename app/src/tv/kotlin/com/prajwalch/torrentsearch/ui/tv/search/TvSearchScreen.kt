package com.prajwalch.torrentsearch.ui.tv.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.tv.theme.spaces
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.search.SearchState
import com.prajwalch.torrentsearch.ui.search.SearchViewModel
import com.prajwalch.torrentsearch.ui.sortCriteriaStringResource
import com.prajwalch.torrentsearch.ui.sortOrderStringResource
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvChip
import com.prajwalch.torrentsearch.ui.tv.component.TvChipRow
import com.prajwalch.torrentsearch.ui.tv.component.TvLoadingState
import com.prajwalch.torrentsearch.ui.tv.component.TvMessageState
import com.prajwalch.torrentsearch.ui.tv.component.TvOptionDialog
import com.prajwalch.torrentsearch.ui.tv.component.TvTorrentCard
import com.prajwalch.torrentsearch.ui.tv.component.TvTopBar
import com.prajwalch.torrentsearch.ui.tv.component.TvTextInput
import com.prajwalch.torrentsearch.ui.tv.component.displayName
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding
import com.prajwalch.torrentsearch.ui.tv.torrentactions.TvTorrentActionsDialog

import org.koin.androidx.compose.koinViewModel

/**
 * TV search results.
 *
 * Differences from the handheld `SearchScreen`:
 *
 * - `PullToRefreshBox` removed; refresh is an explicit top-bar action because a
 *   D-pad has no pull gesture.
 * - The filter strip is a `TvChipRow` instead of a `LazyRow` of dropdown-anchored
 *   `FilterChip`s - dropdown menus have no usable D-pad semantics.
 * - The "scroll to top" FAB is gone; D-pad up from the top row already scrolls.
 * - `imePadding` removed - no soft keyboard inset on TV.
 * - Selecting a torrent opens a focus-trapping [TvTorrentActionsDialog] instead
 *   of a `ModalBottomSheet`.
 */
@Composable
fun TvSearchScreen(
    query: String,
    category: Category,
    onNavigateToTorrentDetails: (id: String, pageUrl: String, providerName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // SearchViewModel pulls query/category from SavedStateHandle, which
    // navigation-compose populates from the typed @Serializable route. No Koin
    // parameters are involved - matching the handheld graph exactly.
    val viewModel: SearchViewModel = koinViewModel()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    // Screen entry point: focus lands on the filter chips when this route opens.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    // Filter-by-name, mirroring the handheld screen's top-bar search toggle: the
    // field appears under the header and filters the list as text is typed.
    var showFilter by remember { mutableStateOf(false) }
    var filterField by remember { mutableStateOf(TextFieldValue()) }
    var sortCriteriaDialog by remember { mutableStateOf(false) }
    var sortOrderDialog by remember { mutableStateOf(false) }
    if (showFilter) {
        LaunchedEffect(Unit) {
            snapshotFlow { filterField.text }
                .drop(1)
                .collectLatest { viewModel.filterSearchResultsByName(it) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(
            title = query,
            subtitle = stringResource(
                R.string.search_results_count_format,
                uiState.torrents.size.toString(),
                query,
                category.displayName(),
            ),
            actions = {
                val isSearching = uiState.searchState is SearchState.ResultsAvailable.Searching
                val resultsAvailable =
                    uiState.searchState is SearchState.ResultsAvailable

                TvActionButton(
                    enabled = resultsAvailable,
                    onClick = {
                        showFilter = !showFilter
                        if (!showFilter) {
                            // Clear the text and the applied filter together, so
                            // reopening the field starts from the full result set.
                            filterField = TextFieldValue()
                            viewModel.filterSearchResultsByName("")
                        }
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (showFilter) R.string.tv_action_filter_close
                            else R.string.tv_action_filter,
                        ),
                    )
                }

                if (sortCriteriaDialog) {
                    TvOptionDialog(
                        title = stringResource(R.string.settings_section_sort_criteria),
                        options = SortCriteria.entries.map { criteria ->
                            Triple(
                                sortCriteriaStringResource(criteria),
                                criteria == uiState.sortOptions.criteria,
                            ) { viewModel.updateSortCriteria(criteria) }
                        },
                        onDismiss = { sortCriteriaDialog = false },
                    )
                }

                if (sortOrderDialog) {
                    TvOptionDialog(
                        title = stringResource(R.string.settings_section_sort_order),
                        options = SortOrder.entries.map { order ->
                            Triple(
                                sortOrderStringResource(order),
                                order == uiState.sortOptions.order,
                            ) { viewModel.updateSortOrder(order) }
                        },
                        onDismiss = { sortOrderDialog = false },
                    )
                }

                TvActionButton(
                    enabled = resultsAvailable,
                    onClick = { sortCriteriaDialog = true },
                ) {
                    Text(text = stringResource(R.string.action_sort))
                }

                TvActionButton(
                    enabled = resultsAvailable,
                    onClick = { sortOrderDialog = true },
                ) {
                    Text(text = stringResource(R.string.settings_section_sort_order))
                }

                if (isSearching) {
                    TvActionButton(onClick = viewModel::stopSearch) {
                        Text(text = stringResource(R.string.search_action_stop_search))
                    }
                }
                TvActionButton(onClick = viewModel::refreshSearchResults) {
                    Text(text = stringResource(R.string.search_action_refresh))
                }
            },
        )

        if (showFilter) {
            TvTextInput(
                value = filterField,
                onValueChange = { filterField = it },
                onSubmit = { viewModel.filterSearchResultsByName(it) },
                label = stringResource(R.string.search_filter_query_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 4.dp),
            )
        }

        TvChipRow(
            chips = listOf(
                TvChip(
                    key = "filter-dead",
                    label = stringResource(R.string.search_filter_chip_dead_torrents),
                    selected = uiState.torrentFilter.showDeadTorrents,
                    onClick = viewModel::toggleDeadTorrents,
                ),
                TvChip(
                    key = "filter-viewed",
                    label = stringResource(R.string.search_filter_chip_hide_viewed),
                    selected = uiState.torrentFilter.hideViewed,
                    onClick = viewModel::toggleHideViewedTorrents,
                ),
            ) + uiState.torrentFilter.providers.map { provider ->
                TvChip(
                    key = "provider-${provider.provider}",
                    label = provider.provider,
                    selected = provider.selected,
                    onClick = { viewModel.toggleSearchProviderResults(provider.provider) },
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 4.dp),
            initialFocusRequester = entryFocusRequester,
        )

        when (val state = uiState.searchState) {
            SearchState.Loading -> TvLoadingState(modifier = Modifier.weight(1f))

            SearchState.InternetError -> TvMessageState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.internet_connection_error),
                actionLabel = stringResource(R.string.button_try_again),
                onAction = viewModel::refreshSearchResults,
            )

            SearchState.ResultsNotFound -> TvMessageState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.search_state_results_not_found_title),
                description = stringResource(
                    R.string.search_state_results_not_found_description,
                    query,
                    category.displayName(),
                ),
                actionLabel = stringResource(R.string.search_action_refresh),
                onAction = viewModel::refreshSearchResults,
            )

            is SearchState.ResultsAvailable -> {
                val torrents = uiState.torrents
                if (torrents.isEmpty()) {
                    TvLoadingState(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        // `weight(1f)`, not `fillMaxSize()`. Inside this Column,
                        // fillMaxSize() hands the list the full screen height *in
                        // addition to* the top bar and chip row, so it extends past
                        // the bottom of the display. Items below the first are then
                        // laid out off-screen and DPAD_DOWN stops being able to reach
                        // them: the list appeared frozen after one card.
                        modifier = Modifier.weight(1f),
                        // Overscan margin plus focus reserve, so the first and last
                        // focused card is never flush against the viewport edge.
                        contentPadding = tvListContentPadding(),
                        verticalArrangement = Arrangement.spacedBy(
                            MaterialTheme.spaces.small,
                        ),
                    ) {
                        items(torrents, key = { it.id }) { torrent ->
                            TvTorrentCard(
                                torrent = torrent,
                                onClick = { selectedTorrent = torrent },
                            )
                        }
                    }
                }
            }
        }
    }

    selectedTorrent?.let { torrent ->
        TvTorrentActionsDialog(
            torrent = torrent,
            onDismiss = { selectedTorrent = null },
            onViewDetails = { id, pageUrl, providerName ->
                selectedTorrent = null
                onNavigateToTorrentDetails(id, pageUrl, providerName)
            },
        )
    }
}