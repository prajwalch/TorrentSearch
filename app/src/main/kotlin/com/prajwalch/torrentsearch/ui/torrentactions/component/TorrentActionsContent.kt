package com.prajwalch.torrentsearch.ui.torrentactions.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.NSFWBadge
import com.prajwalch.torrentsearch.ui.component.TorrentMetadata
import com.prajwalch.torrentsearch.ui.extension.toRelativeTimeSpanString
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.ui.torrentactions.MagnetUriState
import com.prajwalch.torrentsearch.ui.torrentactions.TorrentFileLinkState

@Composable
fun TorrentActionsContent(
    torrent: Torrent,
    magnetUriState: MagnetUriState,
    torrentFileLinkState: TorrentFileLinkState,
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

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            MagnetLinkActionItem(
                magnetUriState = magnetUriState,
                onOpenMagnetLink = onOpenMagnetLink,
                onCopyMagnetLink = onCopyMagnetLink,
                onShareMagnetLink = onShareMagnetLink,
            )

            TorrentFileActionItem(
                linkState = torrentFileLinkState,
                onDownloadTorrentFile = onDownloadTorrentFile,
            )

            DetailsPageActionItem(
                onOpenTorrentDetails = onOpenTorrentDetails,
                onCopyDetailsPageLink = onCopyDetailsPageLink,
                onShareDetailsPageLink = onShareDetailsPageLink,
                enabled = !torrent.descriptionPageUrl.isNullOrBlank(),
            )
        }
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small)) {
                torrent.uploadDate?.let {
                    Text(
                        text = it.toRelativeTimeSpanString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (torrent.isNSFW) NSFWBadge()

                Spacer(Modifier.weight(1f))

                Text(
                    text = torrent.providerName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Text(
                text = torrent.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )

            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                TorrentMetadata(
                    size = torrent.size,
                    seeders = torrent.seeders,
                    peers = torrent.peers,
                )
            }
        }

        IconToggleButton(
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
private fun MagnetLinkActionItem(
    magnetUriState: MagnetUriState,
    onOpenMagnetLink: (String) -> Unit,
    onCopyMagnetLink: (String) -> Unit,
    onShareMagnetLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = magnetUriState is MagnetUriState.Ready) {
                require(magnetUriState is MagnetUriState.Ready)
                onOpenMagnetLink(magnetUriState.value)
            },
        leadingContent = {
            Crossfade(magnetUriState) { targetState ->
                when (targetState) {
                    MagnetUriState.Loading, MagnetUriState.Fetching -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                            strokeWidth = 2.0.dp,
                        )
                    }

                    MagnetUriState.Error -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_error),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }

                    is MagnetUriState.Ready -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_magnet),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.torrent_title_magnet_link),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            Crossfade(magnetUriState) { targetState ->
                val textResId = when (targetState) {
                    MagnetUriState.Loading,
                    MagnetUriState.Fetching,
                        -> R.string.torrent_message_getting_magnet_link

                    MagnetUriState.Error -> R.string.torrent_message_failed_to_get_magnet_link
                    is MagnetUriState.Ready -> R.string.torrent_message_tap_to_open_magnet_link
                }

                val textColor = if (targetState == MagnetUriState.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    LocalContentColor.current
                }

                Text(
                    text = stringResource(textResId),
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = {
            AnimatedVisibility(
                visible = magnetUriState is MagnetUriState.Ready,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                require(magnetUriState is MagnetUriState.Ready)
                val magnetUri = magnetUriState.value

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onCopyMagnetLink(magnetUri) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = { onShareMagnetLink(magnetUri) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(enabled = true),
    )
}

@Composable
private fun TorrentFileActionItem(
    linkState: TorrentFileLinkState,
    onDownloadTorrentFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = linkState is TorrentFileLinkState.Ready) {
                require(linkState is TorrentFileLinkState.Ready)
                onDownloadTorrentFile(linkState.value)
            },
        leadingContent = {
            Crossfade(linkState) { targetState ->
                when (targetState) {
                    TorrentFileLinkState.Preparing,
                    TorrentFileLinkState.WaitingForMagnetUri,
                        -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                            strokeWidth = 2.0.dp,
                        )
                    }

                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.torrent_title_torrent_file),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            Crossfade(linkState) { targetState ->
                Text(
                    text = targetState.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        colors = ListItemDefaults.colors(
            enabled = linkState != TorrentFileLinkState.Unavailable,
        ),
    )
}

@Composable
private fun TorrentFileLinkState.displayName(): String {
    val resId = when (this) {
        TorrentFileLinkState.Preparing -> R.string.torrent_message_preparing_download_link
        TorrentFileLinkState.WaitingForMagnetUri -> R.string.torrent_message_waiting_magnet_link
        TorrentFileLinkState.Unavailable -> R.string.torrent_message_not_available
        is TorrentFileLinkState.Ready -> R.string.torrent_message_tap_to_download_file
    }

    return stringResource(resId)
}

@Composable
private fun DetailsPageActionItem(
    onOpenTorrentDetails: () -> Unit,
    onCopyDetailsPageLink: () -> Unit,
    onShareDetailsPageLink: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenTorrentDetails, enabled = enabled),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_link),
                contentDescription = null,
            )
        },
        headlineContent = {
            Text(
                text = stringResource(R.string.torrent_title_details_page),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            val textResId = if (enabled) {
                R.string.torrent_message_tap_to_open_details
            } else {
                R.string.torrent_message_not_available
            }

            Text(
                text = stringResource(textResId),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            if (enabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopyDetailsPageLink) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = null,
                        )
                    }

                    IconButton(onClick = onShareDetailsPageLink) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(enabled),
    )
}

@Composable
private fun ListItemDefaults.colors(enabled: Boolean): ListItemColors {
    return if (enabled) {
        colors(
            leadingIconColor = MaterialTheme.colorScheme.primary,
        )
    } else {
        with(colors()) {
            copy(
                headlineColor = disabledHeadlineColor,
                leadingIconColor = disabledLeadingIconColor,
            )
        }
    }
}