package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TorrentInfo(
    size: String?,
    seeders: UInt?,
    peers: UInt?,
    uploadDate: String?,
    category: String?,
    uploader: String?,
    lastChecked: String?,
    infoHash: String,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        modifier = modifier,
        title = { Text(stringResource(R.string.torrent_details_title_info)) },
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.spaces.large,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                InfoListItem(
                    icon = R.drawable.ic_storage,
                    label = stringResource(R.string.torrent_details_label_file_size),
                    text = size,
                )
                InfoListItem(
                    icon = R.drawable.ic_upload,
                    label = stringResource(R.string.torrent_details_label_seeders),
                    text = seeders?.toString(),
                )
                InfoListItem(
                    icon = R.drawable.ic_download,
                    label = stringResource(R.string.torrent_details_label_peers),
                    text = peers?.toString(),
                )
                InfoListItem(
                    icon = R.drawable.ic_calendar_month,
                    label = stringResource(R.string.torrent_details_label_upload_date),
                    text = uploadDate,
                )
                InfoListItem(
                    icon = R.drawable.ic_category,
                    label = stringResource(R.string.torrent_details_label_category),
                    text = category,
                )
                InfoListItem(
                    icon = R.drawable.ic_person,
                    label = stringResource(R.string.torrent_details_label_uploader),
                    text = uploader,
                )
                InfoListItem(
                    icon = R.drawable.ic_update,
                    label = stringResource(R.string.torrent_details_label_last_checked),
                    text = lastChecked,
                )
                InfoListItem(
                    icon = R.drawable.ic_info,
                    label = stringResource(R.string.torrent_details_label_info_hash),
                    text = infoHash,
                )
            }
        }
    }
}

@Composable
private fun InfoListItem(
    @DrawableRes icon: Int,
    label: String,
    text: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(icon),
            contentDescription = null,
        )
        Spacer(Modifier.width(MaterialTheme.spaces.small))
        Column(verticalArrangement = Arrangement.Center) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = text ?: stringResource(R.string.torrent_details_message_not_available),
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = if (text == null) FontStyle.Italic else FontStyle.Normal,
            )
        }
    }
}

@Preview
@Composable
private fun TorrentInfoPreview() {
    TorrentInfo(
        size = "1.2 GB",
        seeders = 20U,
        peers = 5U,
        uploadDate = "2025-0506 13:00 GMT+ dkfdfdkfdkfj",
        category = "Movies",
        uploader = "prajwalch",
        lastChecked = "2025-05-06",
        infoHash = "dkfdskfjek3rdfkdjfkdjfkdjfkdjfkdjfdkfj4434fk3k43AAdg",
    )
}