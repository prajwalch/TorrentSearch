package com.prajwalch.torrentsearch.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

private data class TorrentAction(
    @field:DrawableRes val icon: Int,
    @field:StringRes val label: Int,
    val onClick: (() -> Unit)?,
    val enabled: Boolean = true,
)

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
    onDownloadTorrentFile: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val actionWithDismiss = { action: () -> Unit ->
        action()

        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            onDismiss()
        }

        Unit
    }
    val primaryActions = listOf(
        TorrentAction(
            icon = R.drawable.ic_magnet,
            label = R.string.torrent_list_action_download_torrent,
            onClick = onDownloadTorrent,
        ),
        TorrentAction(
            icon = R.drawable.ic_download,
            label = R.string.torrent_list_action_download_torrent_file,
            onClick = onDownloadTorrentFile,
        ),
        TorrentAction(
            icon = R.drawable.ic_copy,
            label = R.string.torrent_list_action_copy_magnet_link,
            onClick = onCopyMagnetLink,
        ),
        TorrentAction(
            icon = R.drawable.ic_share,
            label = R.string.torrent_list_action_share_magnet_link,
            onClick = onShareMagnetLink,
        ),
    )
    val secondaryActions = listOf(
        TorrentAction(
            icon = R.drawable.ic_public,
            label = R.string.torrent_list_action_open_description_page,
            onClick = onOpenDescriptionPage,
            enabled = enableDescriptionPageActions,
        ),
        TorrentAction(
            icon = R.drawable.ic_copy,
            label = R.string.torrent_list_action_copy_description_page_url,
            onClick = onCopyDescriptionPageUrl,
            enabled = enableDescriptionPageActions,
        ),
        TorrentAction(
            icon = R.drawable.ic_share,
            label = R.string.torrent_list_action_share_description_page_url,
            onClick = onShareDescriptionPageUrl,
            enabled = enableDescriptionPageActions,
        ),
    )

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BottomSheetHeader(
            modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
            title = title,
            showNSFWBadge = showNSFWBadge,
        )

        Column(
            modifier = Modifier.padding(all = MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        ) {
            // These two actions are screen specific therefore shouldn't belong here.
            Column {
                onBookmarkTorrent?.let {
                    ActionListItem(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        onClick = { actionWithDismiss(it) },
                        icon = R.drawable.ic_star,
                        label = stringResource(R.string.torrent_list_action_bookmark_torrent),
                        colors = ListItemDefaults.colors(
                            leadingIconColor = MaterialTheme.colorScheme.secondary,
                            headlineColor = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                }
                onDeleteBookmark?.let {
                    ActionListItem(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        onClick = { actionWithDismiss(it) },
                        icon = R.drawable.ic_delete,
                        label = stringResource(R.string.torrent_list_action_delete_bookmark),
                        colors = ListItemDefaults.colors(
                            leadingIconColor = MaterialTheme.colorScheme.error,
                            headlineColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            }
            ActionList(
                actions = primaryActions,
                onInvokeAction = { actionWithDismiss(it) },
            )
            ActionList(
                actions = secondaryActions,
                onInvokeAction = { actionWithDismiss(it) }
            )
        }
    }
}

@Composable
private fun BottomSheetHeader(
    title: String,
    showNSFWBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        if (showNSFWBadge) NSFWBadge()
        Text(
            text = title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider()
    }
}

@Composable
private fun ActionList(
    actions: List<TorrentAction>,
    onInvokeAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        for ((idx, action) in actions.withIndex()) {
            if (action.onClick == null) continue

            val shape = ActionListItemShapes.shape(idx, actions.size)

            ActionListItem(
                modifier = Modifier.clip(shape = shape),
                onClick = { onInvokeAction(action.onClick) },
                icon = action.icon,
                label = stringResource(action.label),
                enabled = action.enabled,
            )
        }
    }
}

private object ActionListItemShapes {
    val DefaultShape: Shape = RectangleShape

    val RoundedShape: CornerBasedShape
        @Composable get() = MaterialTheme.shapes.medium

    @Composable
    fun shape(index: Int, count: Int): Shape {
        val baseRoundedShape = RoundedShape

        return remember(index, count, DefaultShape, baseRoundedShape) {
            when {
                count == 1 -> baseRoundedShape
                index == 0 -> baseRoundedShape.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp),
                )

                index == count - 1 -> baseRoundedShape.copy(
                    topStart = CornerSize(0.dp),
                    topEnd = CornerSize(0.dp),
                )

                else -> DefaultShape
            }
        }
    }
}

@Composable
private fun ActionListItem(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ListItemColors = ListItemDefaults.colors(),
) {
    val baseColors = colors.copy(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    val resolvedColors = if (enabled) {
        baseColors
    } else {
        baseColors.copy(
            headlineColor = baseColors.disabledHeadlineColor,
            leadingIconColor = baseColors.disabledLeadingIconColor,
        )
    }

    ListItem(
        modifier = modifier.clickable(onClick = onClick, enabled = enabled),
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
            )
        },
        headlineContent = {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        colors = resolvedColors,
    )
}

@Preview
@Composable
private fun TorrentActionsBottomSheetPreview() {
    TorrentSearchTheme {
        TorrentActionsBottomSheet(
            onDismiss = {},
            title = "Torrent Actions Bottom Sheet Title",
            onDownloadTorrent = {},
            onCopyMagnetLink = {},
            onShareMagnetLink = {},
            onOpenDescriptionPage = {},
            onCopyDescriptionPageUrl = {},
            onShareDescriptionPageUrl = {},
            showNSFWBadge = true,
            enableDescriptionPageActions = true,
            onDownloadTorrentFile = {},
        )
    }
}