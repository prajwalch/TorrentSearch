package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

import com.prajwalch.torrentsearch.R

private val torrentClients = listOf(
    TorrentClient(
        name = R.string.libretorrent,
        isRecommended = true,
        sources = listOf(
            Source(
                logo = R.drawable.googleplay,
                url = createPlayStoreUrl(packageId = "org.proninyaroslav.libretorrent")
            ),
            Source(
                logo = R.drawable.fdroid,
                url = createFDroidUrl(packageId = "org.proninyaroslav.libretorrent")
            ),
        )
    ),
    TorrentClient(
        name = R.string.aria2,
        sources = listOf(
            Source(
                logo = R.drawable.fdroid,
                url = createFDroidUrl(packageId = "com.gianlu.aria2app"),
            ),
        )
    ),
    TorrentClient(
        name = R.string.gopeed,
        sources = listOf(
            Source(
                logo = R.drawable.github,
                url = createGithubReleaseUrl(username = "GopeedLab", repo = "gopeed"),
            ),
        )
    ),
    TorrentClient(
        name = R.string.pikatorrent,
        sources = listOf(
            Source(
                logo = R.drawable.googleplay,
                url = createPlayStoreUrl(packageId = "com.pikatorrent.PikaTorrent"),
            ),
            Source(
                logo = R.drawable.github,
                url = createGithubReleaseUrl(username = "G-Ray", repo = "pikatorrent"),
            ),
        )
    ),
    TorrentClient(
        name = R.string.torrserve,
        sources = listOf(
            Source(
                logo = R.drawable.fdroid,
                url = createFDroidUrl(packageId = "ru.yourok.torrserve"),
            ),
        )
    ),
)

private data class TorrentClient(
    @param:StringRes
    val name: Int,
    val isRecommended: Boolean = false,
    val sources: List<Source> = emptyList(),
)

private data class Source(
    @param:DrawableRes
    val logo: Int,
    val url: String,
)

@Composable
fun TorrentClientNotFoundDialog(
    onConfirmation: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
    ),
) {
    AlertDialog(
        modifier = modifier.fillMaxWidth(0.9f),
        onDismissRequest = onConfirmation,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(stringResource(R.string.button_done))
            }
        },
        icon = { DialogLeadingIcon() },
        title = { DialogTitle() },
        text = { DialogContent() },
        properties = properties,
    )
}

@Composable
private fun DialogLeadingIcon(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier,
        imageVector = Icons.Default.Info,
        contentDescription = null,
    )
}

@Composable
private fun DialogTitle(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.torrent_client_not_found)
    )
}

@Composable
private fun DialogContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.torrent_client_not_found_main_content))
        TorrentClientList(clients = torrentClients)
    }
}

@Composable
private fun TorrentClientList(clients: List<TorrentClient>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = clients) {
            TorrentClientListItem(
                modifier = modifier,
                client = it,
            )
        }
    }
}

@Composable
private fun TorrentClientListItem(client: TorrentClient, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    val primaryColors = ListItemDefaults.colors(
        containerColor = colorScheme.primaryContainer,
        headlineColor = colorScheme.onPrimaryContainer,
        supportingColor = colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
        trailingIconColor = colorScheme.onPrimaryContainer,
    )
    val secondaryColors = ListItemDefaults.colors(
        containerColor = colorScheme.secondaryContainer,
        headlineColor = colorScheme.onSecondaryContainer,
        trailingIconColor = colorScheme.onSecondaryContainer,
    )
    val listItemColors = if (client.isRecommended) {
        primaryColors
    } else {
        secondaryColors
    }

    ListItem(
        modifier = Modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .height(56.dp)
            .then(modifier),
        headlineContent = {
            Text(
                text = stringResource(client.name),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            if (client.isRecommended) {
                Text(
                    text = stringResource(R.string.recommended_client)
                )
            }
        },
        trailingContent = { TorrentClientSourcesRow(sources = client.sources) },
        colors = listItemColors,
    )
}

@Composable
private fun TorrentClientSourcesRow(sources: List<Source>, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Row(modifier = modifier) {
        sources.forEach {
            IconButton(onClick = { uriHandler.openUri(it.url) }) {
                Icon(
                    painter = painterResource(it.logo),
                    contentDescription = null,
                )
            }
        }
    }
}

private fun createFDroidUrl(packageId: String): String {
    return "https://f-droid.org/en/packages/$packageId"
}


private fun createGithubReleaseUrl(username: String, repo: String): String {
    return "https://github.com/$username/$repo/releases"
}

private fun createPlayStoreUrl(packageId: String): String {
    return "https://play.google.com/store/apps/details?id=$packageId"
}