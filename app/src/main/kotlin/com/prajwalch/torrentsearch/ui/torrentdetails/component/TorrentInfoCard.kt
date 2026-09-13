package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.extension.toRelativeTimeSpanString
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import java.time.Instant

private object TorrentInfoCardDefaults {
    val Shape: RoundedCornerShape = RoundedCornerShape(24.dp)

    val Colors: CardColors
        @Composable get() = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )

    val Border: BorderStroke
        @Composable get() = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

    val LeadingIconSize: Dp = 20.dp

    val InnerContainerShape: CornerBasedShape
        @Composable get() = MaterialTheme.shapes.medium

    val InnerContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
fun TorrentInfoCard(
    size: String?,
    seeders: UInt?,
    peers: UInt?,
    uploadDate: Instant?,
    category: Category?,
    provider: String,
    uploader: String?,
    lastChecked: Instant?,
    infoHash: String,
    onCopyInfoHash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = TorrentInfoCardDefaults.Shape,
        colors = TorrentInfoCardDefaults.Colors,
        border = TorrentInfoCardDefaults.Border,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            TorrentMetadataRow(
                modifier = Modifier.fillMaxWidth(),
                size = size,
                seeders = seeders,
                peers = peers
            )

            TorrentAdditionalInfoColumn(
                modifier = Modifier.fillMaxWidth(),
                uploadDate = uploadDate,
                category = category,
                provider = provider,
                uploader = uploader,
                lastChecked = lastChecked,
            )

            InfoHash(
                modifier = Modifier.fillMaxWidth(),
                hash = infoHash,
                onCopy = onCopyInfoHash
            )
        }
    }
}

@Composable
private fun TorrentMetadataRow(
    size: String?,
    seeders: UInt?,
    peers: UInt?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TorrentMetadataTile(
            modifier = Modifier.weight(1f),
            leadingIcon = R.drawable.ic_storage,
            label = stringResource(R.string.torrent_details_label_file_size),
            value = size,
        )
        TorrentMetadataTile(
            modifier = Modifier.weight(1f),
            leadingIcon = R.drawable.ic_upload,
            label = stringResource(R.string.torrent_details_label_seeders),
            value = seeders?.toString(),
        )
        TorrentMetadataTile(
            modifier = Modifier.weight(1f),
            leadingIcon = R.drawable.ic_download,
            label = stringResource(R.string.torrent_details_label_peers),
            value = peers?.toString(),
        )
    }
}

@Composable
private fun TorrentMetadataTile(
    @DrawableRes leadingIcon: Int,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = TorrentInfoCardDefaults.InnerContainerShape,
        color = TorrentInfoCardDefaults.InnerContainerColor,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spaces.medium),
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.extraSmall,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = Modifier.size(TorrentInfoCardDefaults.LeadingIconSize),
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )

                Text(
                    text = value ?: "-",
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    color = if (value != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TorrentAdditionalInfoColumn(
    uploadDate: Instant?,
    category: Category?,
    provider: String,
    uploader: String?,
    lastChecked: Instant?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = TorrentInfoCardDefaults.InnerContainerShape,
        color = TorrentInfoCardDefaults.InnerContainerColor,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            InfoRow(
                leadingIcon = R.drawable.ic_calendar_month,
                label = stringResource(R.string.torrent_details_label_upload_date),
                value = uploadDate?.toRelativeTimeSpanString(),
            )
            InfoRow(
                leadingIcon = R.drawable.ic_category,
                label = stringResource(R.string.torrent_details_label_category),
                value = category?.let { categoryStringResource(it) },
            )
            InfoRow(
                leadingIcon = R.drawable.ic_hub,
                label = stringResource(R.string.torrent_details_label_provider),
                value = provider,
            )
            InfoRow(
                leadingIcon = R.drawable.ic_person,
                label = stringResource(R.string.torrent_details_label_uploader),
                value = uploader,
            )
            InfoRow(
                leadingIcon = R.drawable.ic_update,
                label = stringResource(R.string.torrent_details_label_last_checked),
                value = lastChecked?.toRelativeTimeSpanString(),
            )
        }
    }
}

@Composable
private fun InfoRow(
    @DrawableRes leadingIcon: Int,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
    ) {
        Icon(
            modifier = Modifier.size(TorrentInfoCardDefaults.LeadingIconSize),
            painter = painterResource(leadingIcon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            modifier = Modifier.weight(1f),
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = value ?: "-",
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            color = if (value != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InfoHash(
    hash: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = TorrentInfoCardDefaults.InnerContainerShape,
        color = TorrentInfoCardDefaults.InnerContainerColor,
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = MaterialTheme.spaces.large,
                    vertical = MaterialTheme.spaces.medium,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(TorrentInfoCardDefaults.LeadingIconSize),
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = stringResource(R.string.torrent_details_label_info_hash),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )

                Text(
                    text = hash,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            FilledTonalIconButton(onClick = onCopy) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview
@Composable
private fun TorrentInfoCardPreview() {
    TorrentSearchTheme {
        TorrentInfoCard(
            size = "1.22 GB",
            seeders = 20U,
            peers = 5U,
            uploadDate = Instant.now(),
            category = Category.Movies,
            provider = "TorrentDownloads",
            uploader = "prajwalch",
            lastChecked = Instant.now(),
            infoHash = "3gh3xb53da9dga3dg8a5g88fhakdf",
            onCopyInfoHash = {},
        )
    }
}