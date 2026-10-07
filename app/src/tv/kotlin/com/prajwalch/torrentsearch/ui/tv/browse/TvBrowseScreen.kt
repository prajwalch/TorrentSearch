package com.prajwalch.torrentsearch.ui.tv.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.browse.BrowseContentState
import com.prajwalch.torrentsearch.ui.browse.BrowseSort
import com.prajwalch.torrentsearch.ui.browse.BrowseViewModel
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvChip
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvChipRow
import com.prajwalch.torrentsearch.ui.tv.component.TvFocusDefaults
import com.prajwalch.torrentsearch.ui.tv.component.TvLoadingState
import com.prajwalch.torrentsearch.ui.tv.component.TvMessageState
import com.prajwalch.torrentsearch.ui.tv.component.TvTorrentCard
import com.prajwalch.torrentsearch.ui.tv.component.TvTopBar
import com.prajwalch.torrentsearch.ui.tv.component.displayName
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding
import com.prajwalch.torrentsearch.ui.tv.settings.TvConfirmDialog
import com.prajwalch.torrentsearch.ui.tv.torrentactions.TvTorrentActionsDialog

import org.koin.androidx.compose.koinViewModel

/**
 * TV browse.
 *
 * The handheld screen composes its filters into a `LazyRow` of dropdown-anchored
 * `FilterChip`s plus a swipe-dismissible "no torrents" state. Here every filter is
 * a plain selectable chip (press OK to toggle) and refresh is a top-bar action,
 * since pull-to-refresh has no D-pad equivalent.
 */
@Composable
fun TvBrowseScreen(
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToTorrentDetails: (id: String, pageUrl: String, providerName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    // Screen entry point: focus lands on the filter chips when this route opens,
    // so the screen starts at the top instead of leaving focus in the rail.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(
            title = stringResource(R.string.browse_screen_title),
            subtitle = uiState.queryParams.category.displayName(),
            actions = {
                TvActionButton(onClick = viewModel::refreshTorrents) {
                    Text(text = stringResource(R.string.search_action_refresh))
                }
            },
        )

        TvChipRow(
            chips = buildList {
                add(
                    TvChip(
                        key = "sort-latest",
                        label = stringResource(R.string.browse_sort_latest),
                        selected = uiState.queryParams.sort == BrowseSort.Latest,
                        onClick = { viewModel.updateBrowseSort(BrowseSort.Latest) },
                    ),
                )
                add(
                    TvChip(
                        key = "sort-top",
                        label = stringResource(R.string.browse_sort_top),
                        selected = uiState.queryParams.sort == BrowseSort.Top,
                        onClick = { viewModel.updateBrowseSort(BrowseSort.Top) },
                    ),
                )
                add(
                    TvChip(
                        key = "dead",
                        label = stringResource(R.string.search_filter_chip_dead_torrents),
                        selected = uiState.viewFilters.deadTorrents,
                        onClick = viewModel::toggleDeadTorrents,
                    ),
                )
                add(
                    TvChip(
                        key = "hide-viewed",
                        label = stringResource(R.string.search_filter_chip_hide_viewed),
                        selected = uiState.viewFilters.hideViewed,
                        onClick = viewModel::toggleHideViewed,
                    ),
                )
            } + uiState.viewFilters.providers.map { provider ->
                TvChip(
                    key = "provider-${provider.provider}",
                    label = provider.provider,
                    selected = provider.selected,
                    onClick = { viewModel.toggleSearchProviderResults(provider.provider) },
                )
            },
            // fillMaxWidth(), not fillMaxSize(). Inside this Column, fillMaxSize()
            // hands the chip row the full screen height, so the results list below
            // measures to zero height and its cards lay out past the bottom edge -
            // the list looked frozen after the first card.
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 4.dp),
            initialFocusRequester = entryFocusRequester,
        )

        when (val state = uiState.contentState) {
            BrowseContentState.Loading -> TvLoadingState(modifier = Modifier.weight(1f))

            BrowseContentState.InternetError -> TvMessageState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.internet_connection_error),
                actionLabel = stringResource(R.string.browse_button_try_again),
                onAction = viewModel::refreshTorrents,
            )

            BrowseContentState.Unavailable -> TvMessageState(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.browse_state_unavailable_title),
                description = stringResource(R.string.browse_state_unavailable_description),
                actionLabel = stringResource(R.string.browse_button_go_to_providers),
                onAction = onNavigateToSearchProviders,
            )

            // The torrent list is carried on uiState.torrents; contentState only
            // reports whether it is trustworthy yet.
            is BrowseContentState.Available -> {
                if (uiState.torrents.isEmpty()) {
                    // Don't leave a bare empty column - it reads as a broken screen.
                    TvMessageState(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.browse_state_unavailable_title),
                        description = stringResource(
                            R.string.browse_state_unavailable_description,
                        ),
                        actionLabel = stringResource(R.string.browse_button_try_again),
                        onAction = viewModel::refreshTorrents,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = tvListContentPadding(),
                        verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
                    ) {
                        items(uiState.torrents, key = { it.id }) { torrent ->
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