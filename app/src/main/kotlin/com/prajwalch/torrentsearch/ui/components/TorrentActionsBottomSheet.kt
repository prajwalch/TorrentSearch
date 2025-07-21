package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentActionsBottomSheet(
    title: String,
    isNSFW: Boolean,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onDeleteBookmark: () -> Unit,
    onDismissRequest: () -> Unit,
    onDownloadTorrent: () -> Unit,
    onCopyMagnetLink: () -> Unit,
    onShareMagnetLink: () -> Unit,
    onOpenDescriptionPage: () -> Unit,
    onCopyDescriptionPageUrl: () -> Unit,
    onShareDescriptionPageUrl: () -> Unit,
    modifier: Modifier = Modifier,
    hasDescriptionPage: Boolean = true,
) {
    // Always expand the sheet to full.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Make the background slightly darker.
    val scrimColor = BottomSheetDefaults.ScrimColor.copy(alpha = 0.5f)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        scrimColor = scrimColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (isNSFW) {
                NSFWTag(style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            item {
                if (!isBookmarked) {
                    BookmarkAction(onClick = {
                        onBookmark()
                        onDismissRequest()
                    })
                } else {
                    DeleteBookmarkAction(onClick = {
                        onDeleteBookmark()
                        onDismissRequest()
                    })
                }

                DownloadAction(onClick = {
                    onDownloadTorrent()
                    onDismissRequest()
                })
            }
            item {
                ShareMagnetLinkAction(onClick = {
                    onShareMagnetLink()
                    onDismissRequest()
                })
            }
            item {
                CopyMagnetLinkAction(onClick = {
                    onCopyMagnetLink()
                    onDismissRequest()
                })
            }

            if (hasDescriptionPage) {
                item {
                    HorizontalDivider()
                    OpenDescriptionPageAction(onClick = {
                        onOpenDescriptionPage()
                        onDismissRequest()
                    })
                }
                item {
                    CopyDescriptionPageUrlAction(onClick = {
                        onCopyDescriptionPageUrl()
                        onDismissRequest()
                    })
                }
                item {
                    ShareDescriptionPageUrlAction(onClick = {
                        onShareDescriptionPageUrl()
                        onDismissRequest()
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_download,
        label = stringResource(R.string.action_download),
        onClick = onClick,
    )
}

@Composable
private fun CopyMagnetLinkAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_copy,
        label = stringResource(R.string.action_copy_magnet_link),
        onClick = onClick,
    )
}

@Composable
private fun ShareMagnetLinkAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_share,
        label = stringResource(R.string.action_share_magnet_link),
        onClick = onClick,
    )
}

@Composable
private fun OpenDescriptionPageAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_public,
        label = stringResource(R.string.action_open_description_page),
        onClick = onClick,
    )
}

@Composable
private fun CopyDescriptionPageUrlAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_copy,
        label = stringResource(R.string.action_copy_description_page_url),
        onClick = onClick,
    )
}

@Composable
private fun ShareDescriptionPageUrlAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_share,
        label = stringResource(R.string.action_share_description_page_url),
        onClick = onClick,
    )
}

@Composable
private fun BookmarkAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_star,
        label = "Bookmark",
        onClick = onClick,
    )
}

@Composable
private fun DeleteBookmarkAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_star_filled,
        label = "Delete bookmark",
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
        containerColor = Color.Unspecified
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