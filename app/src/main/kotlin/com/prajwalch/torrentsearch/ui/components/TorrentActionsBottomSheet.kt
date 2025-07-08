package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
    onDismissRequest: () -> Unit,
    onDownloadClick: () -> Unit,
    onCopyMagnetLinkClick: () -> Unit,
    onShareMagnetLinkClick: () -> Unit,
    onOpenDescriptionPageClick: () -> Unit,
    onCopyDescriptionPageUrlClick: () -> Unit,
    onShareDescriptionPageUrlClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Make the background slightly darker.
    val scrimColor = BottomSheetDefaults.ScrimColor.copy(alpha = 0.5f)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        scrimColor = scrimColor,
    ) {
        Text(
            modifier = Modifier.Companion.padding(horizontal = 16.dp),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.Companion.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.Companion.height(8.dp))
        DownloadAction {
            onDownloadClick()
            onDismissRequest()
        }
        CopyMagnetLinkAction {
            onCopyMagnetLinkClick()
            onDismissRequest()
        }
        ShareMagnetLinkAction {
            onShareMagnetLinkClick()
            onDismissRequest()
        }

        HorizontalDivider()

        OpenDescriptionPageAction {
            onOpenDescriptionPageClick()
            onDismissRequest()
        }
        CopyDescriptionPageUrlAction {
            onCopyDescriptionPageUrlClick()
            onDismissRequest()
        }
        ShareDescriptionPageUrlAction {
            onShareDescriptionPageUrlClick()
            onDismissRequest()
        }
        Spacer(Modifier.Companion.height(8.dp))
    }
}

@Composable
private fun DownloadAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_download,
        label = stringResource(R.string.action_download),
        onClick = onClick,
    )
}

@Composable
private fun CopyMagnetLinkAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_copy,
        label = stringResource(R.string.action_copy_magnet_link),
        onClick = onClick,
    )
}

@Composable
private fun ShareMagnetLinkAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_share,
        label = stringResource(R.string.action_share_magnet_link),
        onClick = onClick,
    )
}

@Composable
private fun OpenDescriptionPageAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_public,
        label = stringResource(R.string.action_open_description_page),
        onClick = onClick,
    )
}

@Composable
private fun CopyDescriptionPageUrlAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Action(
        modifier = modifier,
        leadingIconId = R.drawable.ic_copy,
        label = stringResource(R.string.action_copy_description_page_url),
        onClick = onClick,
    )
}

@Composable
private fun ShareDescriptionPageUrlAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
        colors = ListItemDefaults.colors(containerColor = Color.Unspecified),
        leadingContent = {
            Icon(
                painter = painterResource(leadingIconId),
                contentDescription = label,
            )
        },
        headlineContent = { Text(text = label) },
    )
}