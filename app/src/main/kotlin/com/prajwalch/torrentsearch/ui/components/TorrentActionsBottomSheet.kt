package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
        Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
            TorrentActionsBottomSheetHeader(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spaces.large),
                title = title,
                isNSFW = isNSFW,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
            HorizontalDivider()

            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
            if (isBookmarked) {
                Action(
                    icon = R.drawable.ic_star_filled,
                    label = R.string.torrent_list_action_delete_bookmark,
                    onClick = actionWithDismiss(onDeleteBookmark),
                )
            } else {
                Action(
                    icon = R.drawable.ic_star,
                    label = R.string.torrent_list_action_bookmark_torrent,
                    onClick = actionWithDismiss(onBookmarkTorrent),
                )
            }
            Action(
                icon = R.drawable.ic_download,
                label = R.string.torrent_list_action_download_torrent,
                onClick = actionWithDismiss(onDownloadTorrent),
            )
            Action(
                modifier = modifier,
                icon = R.drawable.ic_copy,
                label = R.string.torrent_list_action_copy_magnet_link,
                onClick = actionWithDismiss(onCopyMagnetLink),
            )
            Action(
                icon = R.drawable.ic_share,
                label = R.string.torrent_list_action_share_magnet_link,
                onClick = actionWithDismiss(onShareMagnetLink),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))

            if (hasDescriptionPage) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
                Action(
                    icon = R.drawable.ic_public,
                    label = R.string.torrent_list_action_open_description_page,
                    onClick = actionWithDismiss(onOpenDescriptionPage),
                )
                Action(
                    icon = R.drawable.ic_copy,
                    label = R.string.torrent_list_action_copy_description_page_url,
                    onClick = actionWithDismiss(onCopyDescriptionPageUrl),
                )
                Action(
                    icon = R.drawable.ic_share,
                    label = R.string.torrent_list_action_share_description_page_url,
                    onClick = actionWithDismiss(onShareDescriptionPageUrl),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
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
}

@Composable
private fun Action(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
    ),
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
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