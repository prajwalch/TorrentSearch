package com.prajwalch.torrentsearch.ui.torrentactions.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.ContentState
import com.prajwalch.torrentsearch.ui.component.ContentStateDefaults
import com.prajwalch.torrentsearch.ui.component.NSFWBadge
import com.prajwalch.torrentsearch.ui.component.TorrentMetadata
import com.prajwalch.torrentsearch.ui.extension.toRelativeTimeSpanString
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.ui.torrentactions.MagnetUriState

@Composable
fun TorrentActionsContent(
    torrent: Torrent,
    magnetUriState: MagnetUriState,
    isTorrentBookmarked: Boolean,
    onToggleBookmark: (Boolean) -> Unit,
    onOpenMagnetLink: (String) -> Unit,
    onDownloadTorrentFile: (String) -> Unit,
    onCopyMagnetLink: (String) -> Unit,
    onShareMagnetLink: (String) -> Unit,
    onOpenTorrentDetails: () -> Unit,
    onCopyDetailsPageLink: () -> Unit,
    onShareDetailsPageLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = MaterialTheme.spaces.large)
            .padding(bottom = MaterialTheme.spaces.large)
            .verticalScroll(state = rememberScrollState())
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        BottomSheetHeader(
            torrent = torrent,
            isBookmarked = isTorrentBookmarked,
            onToggleBookmark = onToggleBookmark,
            enableBookmarkAction = magnetUriState is MagnetUriState.Ready,
        )

        HorizontalDivider()

        TorrentActionColumn(
            magnetUriState = magnetUriState,
            onOpenMagnetLink = onOpenMagnetLink,
            onDownloadTorrentFile = onDownloadTorrentFile,
            onCopyMagnetLink = onCopyMagnetLink,
            onShareMagnetLink = onShareMagnetLink,
            onOpenTorrentDetails = onOpenTorrentDetails,
            onCopyDetailsPageLink = onCopyDetailsPageLink,
            onShareDetailsPageLink = onShareDetailsPageLink,
            enableDetailsAction = torrent.descriptionPageUrl != null,
        )
    }
}

@Composable
private fun BottomSheetHeader(
    torrent: Torrent,
    isBookmarked: Boolean,
    onToggleBookmark: (Boolean) -> Unit,
    enableBookmarkAction: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
    ) {
        Icon(
            painter = painterResource(torrent.category.iconResId()),
            contentDescription = torrent.category?.let { categoryStringResource(it) },
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small)) {
                torrent.uploadDate?.let {
                    Text(
                        text = it.toRelativeTimeSpanString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (torrent.isNSFW) NSFWBadge()

                Spacer(Modifier.weight(1f))

                Text(
                    text = torrent.providerName,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Text(
                text = torrent.name,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )

            TorrentMetadata(
                size = torrent.size,
                seeders = torrent.seeders,
                peers = torrent.peers,
            )
        }

        FilledTonalIconToggleButton(
            checked = isBookmarked,
            onCheckedChange = onToggleBookmark,
            enabled = enableBookmarkAction,
        ) {
            val iconResId = if (isBookmarked) {
                R.drawable.ic_bookmark_check
            } else {
                R.drawable.ic_bookmark
            }

            val contentDescriptionResId = if (isBookmarked) {
                R.string.torrent_action_delete_bookmark
            } else {
                R.string.torrent_action_bookmark_torrent
            }

            Icon(
                painter = painterResource(iconResId),
                contentDescription = stringResource(contentDescriptionResId),
            )
        }
    }
}

@Composable
private fun TorrentActionColumn(
    magnetUriState: MagnetUriState,
    onOpenMagnetLink: (String) -> Unit,
    onDownloadTorrentFile: (String) -> Unit,
    onCopyMagnetLink: (String) -> Unit,
    onShareMagnetLink: (String) -> Unit,
    onOpenTorrentDetails: () -> Unit,
    onCopyDetailsPageLink: () -> Unit,
    onShareDetailsPageLink: () -> Unit,
    enableDetailsAction: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        Crossfade(magnetUriState) { targetMagnetUriState ->
            when (targetMagnetUriState) {
                MagnetUriState.Loading -> MagnetUriLoadingState()
                MagnetUriState.Fetching -> MagnetUriFetchingState()
                MagnetUriState.Error -> MagnetUriErrorState()

                is MagnetUriState.Ready -> {
                    val magnetUri = targetMagnetUriState.value

                    Column(modifier = Modifier.clip(MaterialTheme.shapes.large)) {
                        ActionListItem(
                            onClick = { onOpenMagnetLink(magnetUri) },
                            icon = painterResource(R.drawable.ic_magnet),
                            label = stringResource(R.string.torrent_action_open_magnet_link),
                        )
                        ActionListItem(
                            onClick = { onDownloadTorrentFile(magnetUri) },
                            icon = painterResource(R.drawable.ic_download),
                            label = stringResource(R.string.torrent_action_download_torrent_file),
                        )
                        ActionListItem(
                            onClick = { onCopyMagnetLink(magnetUri) },
                            icon = painterResource(R.drawable.ic_copy),
                            label = stringResource(R.string.torrent_action_copy_magnet_link),
                        )
                        ActionListItem(
                            onClick = { onShareMagnetLink(magnetUri) },
                            icon = painterResource(R.drawable.ic_share),
                            label = stringResource(R.string.torrent_action_share_magnet_link),
                        )
                    }
                }
            }
        }

        Column(modifier = modifier.clip(MaterialTheme.shapes.large)) {
            ActionListItem(
                onClick = onOpenTorrentDetails,
                icon = painterResource(R.drawable.ic_link),
                label = stringResource(R.string.torrent_action_open_description_page),
                enabled = enableDetailsAction,
            )
            ActionListItem(
                onClick = onCopyDetailsPageLink,
                icon = painterResource(R.drawable.ic_copy),
                label = stringResource(R.string.torrent_action_copy_description_page_url),
                enabled = enableDetailsAction,
            )
            ActionListItem(
                onClick = onShareDetailsPageLink,
                icon = painterResource(R.drawable.ic_share),
                label = stringResource(R.string.torrent_action_share_description_page_url),
                enabled = enableDetailsAction,
            )
        }
    }
}

@Composable
private fun MagnetUriLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MagnetUriFetchingState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
            ),
        icon = { CircularProgressIndicator() },
        title = { Text(stringResource(R.string.torrent_message_getting_magnet_link)) },
    )
}

@Composable
private fun MagnetUriErrorState(modifier: Modifier = Modifier) {
    ContentState(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
            ),
        icon = {
            Icon(
                modifier = Modifier.size(ContentStateDefaults.SmallIconSize),
                painter = painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.torrent_message_failed_to_get_magnet_link)) },
    )
}