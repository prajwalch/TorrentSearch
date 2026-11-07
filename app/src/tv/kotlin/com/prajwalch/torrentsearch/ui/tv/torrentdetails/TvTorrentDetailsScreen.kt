package com.prajwalch.torrentsearch.ui.tv.torrentdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme

import coil3.compose.AsyncImage

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.torrentdetails.TorrentDetailsState
import com.prajwalch.torrentsearch.ui.torrentdetails.TorrentDetailsViewModel
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvCardShape
import com.prajwalch.torrentsearch.ui.tv.component.TvLoadingState
import com.prajwalch.torrentsearch.ui.tv.component.TvMessageState
import com.prajwalch.torrentsearch.ui.tv.component.displayName
import com.prajwalch.torrentsearch.ui.tv.component.tvFocusPassive
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding
import com.prajwalch.torrentsearch.ui.tv.theme.spaces

import org.koin.androidx.compose.koinViewModel

/**
 * TV torrent details.
 *
 * Differences from the handheld `TorrentDetailsScreen`:
 *
 * - `PullToRefreshBox` removed - there is no D-pad pull gesture, and the copy and
 *   share top-bar actions are dropped along with the clipboard/share support.
 * - The description renders as plain text. The handheld screen uses the markdown
 *   composable, which is built on `androidx.compose.material3` and therefore does
 *   not pick up the TV `MaterialTheme`; plain text also avoids inline description
 *   images, which are unreadable at 10-foot.
 * - Poster and screenshots are laid out side by side and sized for a 55" panel.
 *   The handheld 240dp poster is about a fifth of a TV's height and effectively
 *   illegible.
 * - The body is a `LazyColumn` of focusable blocks rather than a `verticalScroll`
 *   column of plain text. Nothing on this screen is clickable, so with a plain
 *   scroll column the D-pad had no focus to move and everything below the first
 *   viewport - screenshots, description - was unreachable. Each block takes focus
 *   ([tvFocusPassive]), which both shows the ring and lets focus-driven
 *   scrolling bring the rest of the page into view.
 *
 */
@Composable
fun TvTorrentDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: TorrentDetailsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Screen entry point: focus lands on the header when this route opens, which
    // also gives the rest of the page something to scroll to.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    when (val state = uiState.detailsState) {
        TorrentDetailsState.Loading -> TvLoadingState(modifier = modifier)

        TorrentDetailsState.NoInternetConnection -> TvMessageState(
            modifier = modifier,
            title = stringResource(R.string.internet_connection_error),
            actionLabel = stringResource(R.string.button_try_again),
            onAction = viewModel::loadDetails,
            focusRequester = entryFocusRequester,
        )

        TorrentDetailsState.Unavailable -> TvMessageState(
            modifier = modifier,
            title = stringResource(R.string.torrent_details_state_unavailable_title),
            description = stringResource(
                R.string.torrent_details_state_unavailable_description,
            ),
        )

        is TorrentDetailsState.UnsupportedTorrentSite -> TvMessageState(
            modifier = modifier,
            title = stringResource(
                R.string.torrent_details_state_unsupported_site_title,
            ),
            description = stringResource(
                R.string.torrent_details_state_unsupported_site_description,
                state.host,
            ),
        )

        is TorrentDetailsState.SomethingWentWrong -> TvMessageState(
            modifier = modifier,
            title = stringResource(R.string.torrent_details_state_something_wrong_title),
            description = state.message,
            actionLabel = stringResource(R.string.button_try_again),
            onAction = viewModel::loadDetails,
            focusRequester = entryFocusRequester,
        )

        is TorrentDetailsState.Ready -> {
            val details = state.details

            LazyColumn(
                modifier = modifier,
                // Overscan margin plus focus reserve: the first and last block is
                // never parked flush against the viewport edge, so its ring and
                // scaled body stay inside the visible screen.
                contentPadding = tvListContentPadding(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (entryFocusRequester != null) {
                                    Modifier.focusRequester(entryFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                            .tvFocusPassive(shape = TvCardShape),
                        horizontalArrangement = Arrangement.spacedBy(
                            MaterialTheme.spaces.large,
                        ),
                    ) {
                        details.posterUrl?.let { posterUrl ->
                            AsyncImage(
                                model = posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.aspectRatio(2f / 3f),
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(
                                MaterialTheme.spaces.extraSmall,
                            ),
                        ) {
                            Text(
                                text = details.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            DetailLine(
                                label = stringResource(R.string.torrent_details_label_provider),
                                value = viewModel.providerName,
                            )
                            details.size?.let {
                                DetailLine(
                                    label = stringResource(
                                        R.string.torrent_details_label_file_size,
                                    ),
                                    value = it,
                                )
                            }
                            details.seeders?.let {
                                DetailLine(
                                    label = stringResource(
                                        R.string.torrent_details_label_seeders,
                                    ),
                                    value = it.toString(),
                                )
                            }
                            details.peers?.let {
                                DetailLine(
                                    label = stringResource(
                                        R.string.torrent_details_label_peers,
                                    ),
                                    value = it.toString(),
                                )
                            }
                            details.uploader?.let {
                                DetailLine(
                                    label = stringResource(
                                        R.string.torrent_details_label_uploader,
                                    ),
                                    value = it,
                                )
                            }
                            details.category?.let {
                                DetailLine(
                                    label = stringResource(
                                        R.string.torrent_details_label_category,
                                    ),
                                    value = it.displayName(),
                                )
                            }
                            DetailLine(
                                label = stringResource(
                                    R.string.torrent_details_label_info_hash,
                                ),
                                value = details.infoHash,
                            )
                        }
                    }
                }

                details.screenshotUrls.take(TV_MAX_SCREENSHOTS).forEachIndexed { index, url ->
                    item(key = "screenshot-$index") {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .tvFocusPassive(shape = TvCardShape),
                        )
                    }
                }

                details.description?.let { description ->
                    item(key = "description") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusPassive(shape = TvCardShape),
                            verticalArrangement = Arrangement.spacedBy(
                                MaterialTheme.spaces.extraSmall,
                            ),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.torrent_details_title_description,
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Upper bound on eagerly rendered screenshots - a 4K panel cannot show more. */
private const val TV_MAX_SCREENSHOTS = 4

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            // Info hashes are long unbroken hex strings; ellipsise in the middle so
            // both ends stay readable.
            overflow = TextOverflow.MiddleEllipsis,
        )
    }
}
