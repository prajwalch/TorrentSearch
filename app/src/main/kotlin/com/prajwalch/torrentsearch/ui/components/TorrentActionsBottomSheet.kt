package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentActionsBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    onBookmarkTorrent: () -> Unit,
    onDeleteBookmark: () -> Unit,
    onDownloadTorrent: () -> Unit,
    onCopyMagnetLink: () -> Unit,
    onShareMagnetLink: () -> Unit,
    onOpenDescriptionPage: () -> Unit,
    onCopyDescriptionPageUrl: () -> Unit,
    onShareDescriptionPageUrl: () -> Unit,
    isNSFW: Boolean,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
    hasDescriptionPage: Boolean = true,
) {
    // Always expand the sheet to full.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun actionWithDismiss(action: () -> Unit) = {
        action()
        onDismissRequest()
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        TorrentActionsBottomSheetHeader(
            modifier = Modifier
                .padding(
                    horizontal = MaterialTheme.spaces.large,
                ),
            title = title,
            isNSFW = isNSFW,
        )

        LazyColumn {
            item {
                BookmarkAction(
                    isBookmarked = isBookmarked,
                    onBookmarkTorrent = actionWithDismiss(onBookmarkTorrent),
                    onDeleteBookmark = actionWithDismiss(onDeleteBookmark),
                )
            }
            item { DownloadTorrent(onClick = actionWithDismiss(onDownloadTorrent)) }
            item { ShareMagnetLink(onClick = actionWithDismiss(onShareMagnetLink)) }
            item { CopyMagnetLink(onClick = actionWithDismiss(onCopyMagnetLink)) }
            item { Spacer(modifier = Modifier.height(MaterialTheme.spaces.small)) }

            if (hasDescriptionPage) {
                item { HorizontalDivider() }
                item { Spacer(modifier = Modifier.height(MaterialTheme.spaces.small)) }
                item {
                    OpenDescriptionPage(
                        onClick = actionWithDismiss(onOpenDescriptionPage)
                    )
                }
                item {
                    CopyDescriptionPageUrl(
                        onClick = actionWithDismiss(onCopyDescriptionPageUrl)
                    )
                }
                item {
                    ShareDescriptionPageUrl(
                        onClick = actionWithDismiss(onShareDescriptionPageUrl)
                    )
                }
                item { Spacer(modifier = Modifier.height(MaterialTheme.spaces.small)) }
            }
        }
    }
}

@Composable
private fun TorrentActionsBottomSheetHeader(
    title: String,
    isNSFW: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isNSFW) NSFWBadge()
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
}

@Composable
private fun BookmarkAction(
    isBookmarked: Boolean,
    onBookmarkTorrent: () -> Unit,
    onDeleteBookmark: () -> Unit,
) {
    if (!isBookmarked) {
        BookmarkTorrent(onClick = onBookmarkTorrent)
    } else {
        DeleteBookmark(onClick = onDeleteBookmark)
    }
}

@Composable
private fun BookmarkTorrent(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_star,
        label = stringResource(R.string.action_bookmark_torrent),
        onClick = onClick,
    )
}

@Composable
private fun DeleteBookmark(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_star_filled,
        label = stringResource(R.string.action_delete_bookmark),
        onClick = onClick,
    )
}

@Composable
private fun DownloadTorrent(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_download,
        label = stringResource(R.string.action_download_torrent),
        onClick = onClick,
    )
}

@Composable
private fun CopyMagnetLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_copy,
        label = stringResource(R.string.action_copy_magnet_link),
        onClick = onClick,
    )
}

@Composable
private fun ShareMagnetLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_share,
        label = stringResource(R.string.action_share_magnet_link),
        onClick = onClick,
    )
}

@Composable
private fun OpenDescriptionPage(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_public,
        label = stringResource(R.string.action_open_description_page),
        onClick = onClick,
    )
}

@Composable
private fun CopyDescriptionPageUrl(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_copy,
        label = stringResource(R.string.action_copy_description_page_url),
        onClick = onClick,
    )
}

@Composable
private fun ShareDescriptionPageUrl(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_share,
        label = stringResource(R.string.action_share_description_page_url),
        onClick = onClick,
    )
}

@Composable
private fun Action(
    @DrawableRes leadingIconId: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = Color.Transparent
    ),
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
        leadingContent = {
            Icon(
                painter = painterResource(leadingIconId),
                contentDescription = label,
            )
        },
        headlineContent = { Text(text = label) },
        colors = colors,
    )
}