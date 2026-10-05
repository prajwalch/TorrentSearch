package com.prajwalch.torrentsearch.ui.tv.torrentdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme

import coil3.compose.AsyncImage

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.torrentdetails.TorrentDetailsState
import com.prajwalch.torrentsearch.ui.torrentdetails.TorrentDetailsViewModel
import com.prajwalch.torrentsearch.ui.tv.component.TvLoadingState
import com.prajwalch.torrentsearch.ui.tv.component.TvMessageState
import com.prajwalch.torrentsearch.ui.tv.component.displayName
import com.prajwalch.torrentsearch.ui.tv.component.tvOverscanPadding
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
 */
@Composable
fun TvTorrentDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: TorrentDetailsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        when (val state = uiState.detailsState) {
            TorrentDetailsState.Loading -> TvLoadingState(modifier = Modifier.fillMaxWidth())

            TorrentDetailsState.NoInternetConnection -> TvMessageState(
                title = stringResource(R.string.internet_connection_error),
                actionLabel = stringResource(R.string.button_try_again),
                onAction = viewModel::loadDetails,
            )

            TorrentDetailsState.Unavailable -> TvMessageState(
                title = stringResource(R.string.torrent_details_state_unavailable_title),
                description = stringResource(
                    R.string.torrent_details_state_unavailable_description,
                ),
            )

            is TorrentDetailsState.UnsupportedTorrentSite -> TvMessageState(
                title = stringResource(
                    R.string.torrent_details_state_unsupported_site_title,
                ),
                description = stringResource(
                    R.string.torrent_details_state_unsupported_site_description,
                    state.host,
                ),
            )

            is TorrentDetailsState.SomethingWentWrong -> TvMessageState(
                title = stringResource(
                    R.string.torrent_details_state_something_wrong_title,
                ),
                description = state.message,
                actionLabel = stringResource(R.string.button_try_again),
                onAction = viewModel::loadDetails,
            )

            is TorrentDetailsState.Ready -> {
                val details = state.details

                Row(
                    modifier = Modifier.fillMaxWidth().padding(tvOverscanPadding()),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
                ) {
                    details.posterUrl?.let { posterUrl ->
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxHeight().aspectRatio(2f / 3f),
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
                                label = stringResource(R.string.torrent_details_label_file_size),
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
                                label = stringResource(R.string.torrent_details_label_peers),
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
                            label = stringResource(R.string.torrent_details_label_info_hash),
                            value = details.infoHash,
                        )
                    }
                }

                details.screenshotUrls.take(TV_MAX_SCREENSHOTS).forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                            .aspectRatio(16f / 9f),
                    )
                }

                details.description?.let { description ->
                    Column(
                        modifier = Modifier.padding(tvOverscanPadding()),
                        verticalArrangement = Arrangement.spacedBy(
                            MaterialTheme.spaces.extraSmall,
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.torrent_details_title_description,
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
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

