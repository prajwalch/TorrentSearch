package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    showNSFWBadge: Boolean,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
    enableDescriptionPageActions: Boolean = true,
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
        Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
            TorrentActionsBottomSheetHeader(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spaces.large),
                title = title,
                isNSFW = showNSFWBadge,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
            HorizontalDivider()

            ActionsGroup {
                if (isBookmarked) {
                    DeleteBookmarkAction(onClick = actionWithDismiss(onDeleteBookmark))
                } else {
                    BookmarkTorrentAction(onClick = actionWithDismiss(onBookmarkTorrent))
                }
                DownloadTorrentAction(onClick = actionWithDismiss(onDownloadTorrent))
            }
            HorizontalDivider()
            MagnetLinkActions(
                onCopyMagnetLink = actionWithDismiss(onCopyMagnetLink),
                onShareMagnetLink = actionWithDismiss(onShareMagnetLink),
            )
            HorizontalDivider()
            DescriptionPageActions(
                onOpenDescriptionPage = actionWithDismiss(onOpenDescriptionPage),
                onCopyDescriptionPageUrl = actionWithDismiss(onCopyDescriptionPageUrl),
                onShareDescriptionPageUrl = actionWithDismiss(onShareDescriptionPageUrl),
                enabled = enableDescriptionPageActions,
            )
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
}

@Composable
private fun BookmarkTorrentAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        icon = R.drawable.ic_star,
        label = R.string.torrent_list_action_bookmark_torrent,
        onClick = onClick,
    )
}

@Composable
private fun DeleteBookmarkAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        icon = R.drawable.ic_star_filled,
        label = R.string.torrent_list_action_delete_bookmark,
        onClick = onClick,
    )
}

@Composable
private fun DownloadTorrentAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        icon = R.drawable.ic_download,
        label = R.string.torrent_list_action_download_torrent,
        onClick = onClick,
    )
}

@Composable
private fun MagnetLinkActions(
    onCopyMagnetLink: () -> Unit,
    onShareMagnetLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionsGroup(modifier = modifier) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_copy,
            label = R.string.torrent_list_action_copy_magnet_link,
            onClick = onCopyMagnetLink,
        )
        Action(
            icon = R.drawable.ic_share,
            label = R.string.torrent_list_action_share_magnet_link,
            onClick = onShareMagnetLink,
        )
    }
}

@Composable
private fun DescriptionPageActions(
    onOpenDescriptionPage: () -> Unit,
    onCopyDescriptionPageUrl: () -> Unit,
    onShareDescriptionPageUrl: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ActionsGroup(modifier = modifier) {
        Action(
            icon = R.drawable.ic_public,
            label = R.string.torrent_list_action_open_description_page,
            onClick = onOpenDescriptionPage,
            enabled = enabled,
        )
        Action(
            icon = R.drawable.ic_copy,
            label = R.string.torrent_list_action_copy_description_page_url,
            onClick = onCopyDescriptionPageUrl,
            enabled = enabled,
        )
        Action(
            icon = R.drawable.ic_share,
            label = R.string.torrent_list_action_share_description_page_url,
            onClick = onShareDescriptionPageUrl,
            enabled = enabled,
        )
    }
}

@Composable
private fun Action(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
    ),
) {
    val contentColor = if (enabled) {
        Color.Unspecified
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val colors = colors.copy(
        headlineColor = contentColor,
        leadingIconColor = contentColor,
    )
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick, enabled = enabled)
            .then(modifier),
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(label),
            )
        },
        headlineContent = { Text(text = stringResource(label)) },
        colors = colors,
    )
}

@Composable
private fun ActionsGroup(
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Column(
        modifier = modifier.padding(vertical = MaterialTheme.spaces.small),
        content = content,
    )
}