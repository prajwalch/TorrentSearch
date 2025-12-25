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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentActionsBottomSheet(
    onDismiss: () -> Unit,
    title: String,
    onDownloadTorrent: () -> Unit,
    onCopyMagnetLink: () -> Unit,
    onShareMagnetLink: () -> Unit,
    onOpenDescriptionPage: () -> Unit,
    onCopyDescriptionPageUrl: () -> Unit,
    onShareDescriptionPageUrl: () -> Unit,
    showNSFWBadge: Boolean,
    modifier: Modifier = Modifier,
    enableDescriptionPageActions: Boolean = true,
    onBookmarkTorrent: (() -> Unit)? = null,
    onDeleteBookmark: (() -> Unit)? = null,
) {
    // Always expand the sheet to full.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun hideSheet() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            onDismiss()
        }
    }

    fun actionWithDismiss(action: () -> Unit) = {
        action()
        hideSheet()
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        TorrentActionsBottomSheetHeader(
            modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
            title = title,
            isNSFW = showNSFWBadge,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
        HorizontalDivider()

        ActionsGroup {
            onBookmarkTorrent?.let {
                Actions.BookmarkTorrent(onClick = actionWithDismiss(it))
            }
            onDeleteBookmark?.let {
                Actions.DeleteBookmark(onClick = actionWithDismiss(it))
            }
            Actions.DownloadTorrent(onClick = actionWithDismiss(onDownloadTorrent))
        }
        HorizontalDivider()
        ActionsGroup {
            Actions.CopyMagnetLink(onClick = actionWithDismiss(onCopyMagnetLink))
            Actions.ShareMagnetLink(onClick = actionWithDismiss(onShareMagnetLink))
        }
        HorizontalDivider()
        ActionsGroup {
            Actions.OpenDescriptionPage(
                onClick = actionWithDismiss(onOpenDescriptionPage),
                enabled = enableDescriptionPageActions,
            )
            Actions.CopyDescriptionPageUrl(
                onClick = actionWithDismiss(onCopyDescriptionPageUrl),
                enabled = enableDescriptionPageActions,
            )
            Actions.ShareDescriptionPageUrl(
                onClick = actionWithDismiss(onShareDescriptionPageUrl),
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

object Actions {
    @Composable
    fun BookmarkTorrent(onClick: () -> Unit, modifier: Modifier = Modifier) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_star,
            label = R.string.torrent_list_action_bookmark_torrent,
            onClick = onClick,
        )
    }

    @Composable
    fun DeleteBookmark(onClick: () -> Unit, modifier: Modifier = Modifier) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_star_filled,
            label = R.string.torrent_list_action_delete_bookmark,
            onClick = onClick,
        )
    }

    @Composable
    fun DownloadTorrent(onClick: () -> Unit, modifier: Modifier = Modifier) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_download,
            label = R.string.torrent_list_action_download_torrent,
            onClick = onClick,
        )
    }

    @Composable
    fun CopyMagnetLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_copy,
            label = R.string.torrent_list_action_copy_magnet_link,
            onClick = onClick,
        )
    }

    @Composable
    fun ShareMagnetLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_share,
            label = R.string.torrent_list_action_share_magnet_link,
            onClick = onClick,
        )
    }

    @Composable
    fun OpenDescriptionPage(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    ) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_public,
            label = R.string.torrent_list_action_open_description_page,
            onClick = onClick,
            enabled = enabled,
        )
    }

    @Composable
    fun CopyDescriptionPageUrl(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    ) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_copy,
            label = R.string.torrent_list_action_copy_description_page_url,
            onClick = onClick,
            enabled = enabled,
        )
    }

    @Composable
    fun ShareDescriptionPageUrl(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    ) {
        Action(
            modifier = modifier,
            icon = R.drawable.ic_share,
            label = R.string.torrent_list_action_share_description_page_url,
            onClick = onClick,
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