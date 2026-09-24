package com.prajwalch.torrentsearch.ui.torrentactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.NSFWBadge
import com.prajwalch.torrentsearch.ui.component.TorrentMetadata
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.extension.openMagnetLink
import com.prajwalch.torrentsearch.ui.extension.startTextShareIntent
import com.prajwalch.torrentsearch.ui.extension.toRelativeTimeSpanString
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentActionsBottomSheet(
    onDismiss: () -> Unit,
    torrent: Torrent,
    onTorrentClientNotFound: () -> Unit,
    onNavigateToDetails: (id: String, pageUrl: String, providerName: String) -> Unit,
    onShowSnackBar: (message: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentActionsViewModel = koinViewModel(
        viewModelStoreOwner = rememberViewModelStoreOwner(),
        parameters = { parametersOf(torrent) },
    ),
) {
    val magnetUriState by viewModel.magnetUriState.collectAsStateWithLifecycle()
    val torrentFileState by viewModel.torrentFileState.collectAsStateWithLifecycle()
    val isTorrentBookmarked by viewModel.isTorrentBookmarked.collectAsStateWithLifecycle()
    val openTorrentDetailsInApp by viewModel.openTorrentDetailsInApp.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val createTorrentFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TorrentSearchConstants.MIME_TYPE_TORRENT),
    ) { fileUri ->
        fileUri
            ?.let(contentResolver::openOutputStream)
            ?.let(viewModel::writeTorrentFileContent)
    }

    fun closeSheet() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            onDismiss()
        }
    }

    fun withDetailsPageUrl(action: (String) -> Unit): () -> Unit = {
        torrent.descriptionPageUrl?.let(action)
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val magnetLinkCopiedMessage = stringResource(R.string.torrent_message_magnet_link_copied)
        val urlCopiedMessage = stringResource(R.string.torrent_message_url_copied)

        BottomSheetContent(
            torrent = torrent,
            magnetUriState = magnetUriState,
            torrentFileState = torrentFileState,
            isTorrentBookmarked = isTorrentBookmarked,
            onToggleBookmark = { viewModel.toggleBookmark(it) },
            onOpenMagnetLink = { magnetUri ->
                if (!context.openMagnetLink(magnetUri)) {
                    onTorrentClientNotFound()
                }

                closeSheet()
            },
            onDownloadTorrentFile = { url -> viewModel.downloadTorrentFile(url) },
            onCreateTorrentFile = { fileName -> createTorrentFileLauncher.launch(fileName) },
            onCopyMagnetLink = { magnetUri ->
                coroutineScope.launch {
                    clipboard.copyText(magnetUri)
                    onShowSnackBar(magnetLinkCopiedMessage)
                    closeSheet()
                }
            },
            onShareMagnetLink = { magnetUri ->
                context.startTextShareIntent(magnetUri)
                closeSheet()
            },
            onOpenTorrentDetails = withDetailsPageUrl {
                if (openTorrentDetailsInApp) {
                    onNavigateToDetails(torrent.id, it, torrent.providerName)
                } else {
                    uriHandler.openUri(it)
                }

                closeSheet()
            },
            onCopyDetailsPageLink = withDetailsPageUrl {
                coroutineScope.launch {
                    clipboard.copyText(it)
                    onShowSnackBar(urlCopiedMessage)
                    closeSheet()
                }
            },
            onShareDetailsPageLink = withDetailsPageUrl {
                context.startTextShareIntent(it)
                closeSheet()
            },
        )
    }
}

@Composable
private fun BottomSheetContent(
    torrent: Torrent,
    magnetUriState: MagnetUriState,
    torrentFileState: TorrentFileState,
    isTorrentBookmarked: Boolean,
    onToggleBookmark: (Boolean) -> Unit,
    onOpenMagnetLink: (String) -> Unit,
    onDownloadTorrentFile: (String) -> Unit,
    onCreateTorrentFile: (String) -> Unit,
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
                state = torrentFileState,
                onDownloadTorrentFile = onDownloadTorrentFile,
                onCreateTorrentFile = onCreateTorrentFile,
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
            .clickable(
                enabled = magnetUriState is MagnetUriState.Ready,
                role = Role.Button,
            ) {
                if (magnetUriState is MagnetUriState.Ready) {
                    onOpenMagnetLink(magnetUriState.value)
                }
            },
        leadingContent = {
            Crossfade(
                targetState = magnetUriState,
                label = "Magnet link action leading icon animation",
            ) { targetState ->
                when (targetState) {
                    MagnetUriState.Loading -> LoadingIndicator()

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
            Crossfade(
                modifier = Modifier.fillMaxWidth(),
                targetState = magnetUriState,
                label = "Magnet link action supporting text animation",
            ) { targetState ->
                Text(
                    text = targetState.displayName(),
                    color = if (targetState == MagnetUriState.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        LocalContentColor.current
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = {
            AnimatedVisibility(
                visible = magnetUriState is MagnetUriState.Ready,
                enter = fadeIn(),
                exit = fadeOut(),
                label = "Magnet link action copy and share buttons animation",
            ) {
                val magnetUri = (magnetUriState as? MagnetUriState.Ready)?.value

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { magnetUri?.let(onCopyMagnetLink) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = null,
                        )
                    }

                    IconButton(onClick = { magnetUri?.let(onShareMagnetLink) }) {
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
private fun MagnetUriState.displayName(): String {
    val resId = when (this) {
        MagnetUriState.Loading -> R.string.torrent_message_getting_magnet_link
        MagnetUriState.Error -> R.string.torrent_message_failed_to_get_magnet_link
        is MagnetUriState.Ready -> R.string.torrent_message_tap_to_open_magnet_link
    }

    return stringResource(resId)
}

@Composable
private fun TorrentFileActionItem(
    state: TorrentFileState,
    onDownloadTorrentFile: (String) -> Unit,
    onCreateTorrentFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(
                enabled = state is TorrentFileState.LinkReady,
                role = Role.Button,
            ) {
                if (state is TorrentFileState.LinkReady) {
                    onDownloadTorrentFile(state.value)
                }
            },
        leadingContent = {
            Crossfade(
                targetState = state,
                label = "Torrent file action leading icon animation",
            ) { targetState ->
                when (targetState) {
                    TorrentFileState.PreparingLink,
                    TorrentFileState.WaitingForMagnetUri,
                    TorrentFileState.Downloading,
                    TorrentFileState.WritingContent,
                        -> {
                        LoadingIndicator()
                    }

                    is TorrentFileState.LinkReady,
                    TorrentFileState.LinkUnavailable,
                        -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = null,
                        )
                    }

                    TorrentFileState.DownloadError,
                    TorrentFileState.FileNotFound,
                        -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_error),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }

                    is TorrentFileState.DownloadComplete -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_download_done),
                            contentDescription = null,
                        )
                    }

                    TorrentFileState.ContentWriteComplete -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder_check),
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
            Crossfade(
                modifier = Modifier.fillMaxWidth(),
                targetState = state,
                label = "Torrent file action supporting text animation",
            ) { targetState ->
                val isError = (targetState == TorrentFileState.DownloadError) ||
                        (targetState == TorrentFileState.FileNotFound)

                Text(
                    text = targetState.displayName(),
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        LocalContentColor.current
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = {
            AnimatedVisibility(
                visible = state is TorrentFileState.DownloadComplete,
                enter = fadeIn(),
                exit = fadeOut(),
                label = "Torrent file action save button animation",
            ) {
                IconButton(onClick = {
                    if (state is TorrentFileState.DownloadComplete) {
                        onCreateTorrentFile(state.fileName)
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = stringResource(R.string.torrent_button_save_to_file),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            enabled = state != TorrentFileState.LinkUnavailable,
        ),
    )
}

@Composable
private fun TorrentFileState.displayName(): String {
    val resId = when (this) {
        TorrentFileState.PreparingLink -> R.string.torrent_message_preparing_download_link
        TorrentFileState.WaitingForMagnetUri -> R.string.torrent_message_waiting_magnet_link
        TorrentFileState.LinkUnavailable -> R.string.torrent_message_not_available
        is TorrentFileState.LinkReady -> R.string.torrent_message_tap_to_download_file
        TorrentFileState.Downloading -> R.string.torrent_message_file_downloading
        TorrentFileState.DownloadError -> R.string.torrent_message_file_download_failed
        TorrentFileState.FileNotFound -> R.string.torrent_message_file_not_found
        is TorrentFileState.DownloadComplete -> R.string.torrent_message_file_download_complete
        TorrentFileState.WritingContent -> R.string.torrent_message_file_saving
        TorrentFileState.ContentWriteComplete -> R.string.torrent_message_file_saved
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
            .clickable(
                onClick = onOpenTorrentDetails,
                enabled = enabled,
                role = Role.Button,
            ),
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
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(24.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        trackColor = MaterialTheme.colorScheme.primaryContainer,
        strokeWidth = 2.0.dp,
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