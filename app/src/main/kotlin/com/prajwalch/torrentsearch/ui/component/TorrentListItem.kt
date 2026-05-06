package com.prajwalch.torrentsearch.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.extension.toDisplayDate
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import java.time.Instant

@Composable
fun TorrentListItem(
    name: String,
    size: String,
    seeders: UInt,
    peers: UInt,
    uploadDate: Instant?,
    category: Category?,
    providerName: String,
    isNSFW: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            Icon(
                painter = painterResource(category.iconResId()),
                contentDescription = null,
            )
        },
        overlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    uploadDate?.let { Text(it.toDisplayDate()) }
                    if (isNSFW) NSFWBadge()
                }
                Text(providerName)
            }
        },
        headlineContent = {
            Text(
                text = name,
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            TorrentMetadata(
                size = size,
                seeders = seeders,
                peers = peers,
            )
        },
    )
}

@Composable
private fun TorrentMetadata(
    size: String,
    seeders: UInt,
    peers: UInt,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TorrentMetadataItem(text = { TorrentMetadataText(size) })
        BulletPoint()
        TorrentMetadataItem(
            icon = { TorrentMetadataIcon(R.drawable.ic_upload) },
            text = {
                TorrentMetadataText(
                    text = seeders.toString(),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            contentColor = MaterialTheme.colorScheme.secondary,
        )
        BulletPoint()
        TorrentMetadataItem(
            icon = { TorrentMetadataIcon(R.drawable.ic_download) },
            text = { TorrentMetadataText(peers.toString()) },
            contentColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun TorrentMetadataItem(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    contentColor: Color = LocalContentColor.current,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon?.let { it() }
            text()
        }
    }
}

@Composable
private fun TorrentMetadataIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        modifier = modifier.size(16.dp),
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
    )
}

@Composable
private fun TorrentMetadataText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Text(
        modifier = modifier,
        text = text,
        fontWeight = fontWeight,
        style = style,
    )
}

@Composable
private fun BulletPoint(modifier: Modifier = Modifier) {
    Text(modifier = modifier, text = "\u2022")
}

@Preview
@Composable
private fun TorrentListItemDarkPreview() {
    TorrentSearchTheme(darkTheme = true) {
        TorrentListItem(
            name = "Sinners.2025.1080p [Yts] [H.265] something",
            size = "2.1 GB",
            seeders = 200u,
            peers = 5000u,
            uploadDate = Instant.now(),
            category = Category.Movies,
            providerName = "UIndex",
            isNSFW = false,
        )
    }
}

@Preview
@Composable
private fun TorrentListItemLightPreview() {
    TorrentSearchTheme {
        TorrentListItem(
            name = "Sinners.2025.1080p [Yts] [H.265] something",
            size = "2.1 GB",
            seeders = 200u,
            peers = 5000u,
            uploadDate = Instant.now(),
            category = Category.Movies,
            providerName = "UIndex",
            isNSFW = false,
        )
    }
}