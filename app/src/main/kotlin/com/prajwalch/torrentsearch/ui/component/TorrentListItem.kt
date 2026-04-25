package com.prajwalch.torrentsearch.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun TorrentListItem(
    name: String,
    size: String,
    seeders: UInt,
    peers: UInt,
    uploadDate: String,
    category: Category?,
    providerName: String,
    isNSFW: Boolean,
    modifier: Modifier = Modifier,
    isViewed: Boolean = false,
) {
    val contentAlpha = if (isViewed) 0.6f else 1f

    ListItem(
        modifier = modifier.alpha(contentAlpha),
        leadingContent = {
            Icon(
                painter = painterResource(category.iconResId()),
                contentDescription = null,
            )
        },
        overlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(providerName)

                if (isNSFW) {
                    BulletPoint()
                    NSFWBadge()
                }
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
        trailingContent = { Text(uploadDate) },
        supportingContent = {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "tnum",
                ),
            ) {
                TorrentMetadata(size = size, seeders = seeders, peers = peers)
            }
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
        TorrentMetadataItem(text = size)
        BulletPoint()
        TorrentMetadataItem(
            icon = R.drawable.ic_upload,
            text = seeders.toString(),
            color = MaterialTheme.colorScheme.secondary,
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
        )
        BulletPoint()
        TorrentMetadataItem(
            icon = R.drawable.ic_download,
            text = peers.toString(),
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun TorrentMetadataItem(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    color: Color = LocalContentColor.current,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(it),
                contentDescription = null,
                tint = color,
            )
        }
        Text(text = text, color = color, style = textStyle)
    }
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
            uploadDate = "13 Apr 2025",
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
            uploadDate = "13 Apr 2025",
            category = Category.Movies,
            providerName = "UIndex",
            isNSFW = false,
        )
    }
}