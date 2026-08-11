package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.component.CategoryBadge
import com.prajwalch.torrentsearch.ui.component.SearchProviderBadge
import com.prajwalch.torrentsearch.ui.extension.toDisplayDate
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import java.time.Instant

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            width = 1.0.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(vertical = MaterialTheme.spaces.large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoColumn(
                modifier = Modifier.weight(1f),
                leadingIcon = R.drawable.ic_storage,
                label = stringResource(R.string.torrent_details_label_file_size),
                value = {
                    OptionalInfoText(
                        text = size,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                },
            )
            VerticalDivider()
            InfoColumn(
                modifier = Modifier.weight(1f),
                leadingIcon = R.drawable.ic_upload,
                label = stringResource(R.string.torrent_details_label_seeders),
                value = {
                    OptionalInfoText(
                        text = seeders?.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                },
            )
            VerticalDivider()
            InfoColumn(
                modifier = Modifier.weight(1f),
                leadingIcon = R.drawable.ic_download,
                label = stringResource(R.string.torrent_details_label_peers),
                value = {
                    OptionalInfoText(
                        text = peers?.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                },
            )
        }
        HorizontalDivider()
        Column(
            modifier = Modifier.padding(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            InfoRow(
                leadingIcon = R.drawable.ic_calendar_month,
                label = stringResource(R.string.torrent_details_label_upload_date),
                value = { OptionalInfoText(uploadDate?.toDisplayDate()) },
            )
            InfoRow(
                leadingIcon = R.drawable.ic_category,
                label = stringResource(R.string.torrent_details_label_category),
                value = { category?.let { CategoryBadge(it) } ?: InfoNotAvailable() },
            )
            InfoRow(
                leadingIcon = R.drawable.ic_travel_explore,
                label = stringResource(R.string.torrent_details_label_provider),
                value = { SearchProviderBadge(provider) },
            )
            InfoRow(
                leadingIcon = R.drawable.ic_person,
                label = stringResource(R.string.torrent_details_label_uploader),
                value = { OptionalInfoText(uploader) },
            )
            InfoRow(
                leadingIcon = R.drawable.ic_update,
                label = stringResource(R.string.torrent_details_label_last_checked),
                value = { OptionalInfoText(lastChecked?.toDisplayDate()) },
            )
        }
        HorizontalDivider()
        InfoHash(hash = infoHash, onCopy = onCopyInfoHash)
    }
}

@Composable
private fun InfoColumn(
    @DrawableRes leadingIcon: Int,
    label: String,
    value: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InfoLeadingIcon(leadingIcon)
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                content = value,
            )
        }
    }
}

@Composable
private fun InfoRow(
    @DrawableRes leadingIcon: Int,
    label: String,
    value: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
    ) {
        InfoLeadingIcon(leadingIcon)
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            content = value,
        )
    }
}

@Composable
private fun InfoHash(
    hash: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(MaterialTheme.spaces.large),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfoLeadingIcon(R.drawable.ic_info)

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

@Composable
private fun InfoLeadingIcon(@DrawableRes resId: Int, modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier.size(20.dp),
        painter = painterResource(resId),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OptionalInfoText(
    text: String?,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    text?.let {
        Text(
            modifier = modifier,
            text = it,
            overflow = overflow,
            maxLines = maxLines
        )
    } ?: InfoNotAvailable(modifier)
}

@Composable
private fun InfoNotAvailable(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "-",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun TorrentInfoCardPreview() {
    TorrentSearchTheme {
        TorrentInfoCard(
            size = "1.222222222222 GB",
            seeders = 20U,
            peers = 5U,
            uploadDate = Instant.now(),
            category = Category.Movies,
            provider = "Provider name",
            uploader = "prajwalch",
            lastChecked = Instant.now(),
            infoHash = "dkfdskfjek3rdfkdjfkdjfkdjfkdjfkdjfdkfj4434fk3k43AAdg",
            onCopyInfoHash = {},
        )
    }
}