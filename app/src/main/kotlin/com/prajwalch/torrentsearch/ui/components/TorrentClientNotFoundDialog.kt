package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prajwalch.torrentsearch.R

private val recommendedClients = listOf(
    TorrentClient(
        name = R.string.libretorrent,
        url = createPlayStoreUrl(packageId = "org.proninyaroslav.libretorrent"),
        source = R.string.source_playstore,
    ),
    TorrentClient(
        name = R.string.libretorrent,
        url = createFDroidUrl(packageId = "org.proninyaroslav.libretorrent"),
        source = R.string.source_fdroid,
    ),
)

private val otherClients = listOf(
    // Aria2App.
    TorrentClient(
        name = R.string.aria2,
        url = createFDroidUrl(packageId = "com.gianlu.aria2app"),
        source = R.string.source_fdroid
    ),
    // Gopeed.
    TorrentClient(
        name = R.string.gopeed,
        url = createGithubReleaseUrl(username = "GopeedLab", repo = "gopeed"),
        source = R.string.source_github,
    ),
    // PikaTorrent.
    TorrentClient(
        name = R.string.pikatorrent,
        url = createPlayStoreUrl(packageId = "com.pikatorrent.PikaTorrent"),
        source = R.string.source_playstore,
    ),
    TorrentClient(
        name = R.string.pikatorrent,
        url = createGithubReleaseUrl(username = "G-Ray", repo = "pikatorrent"),
        source = R.string.source_github,
    ),
    // TorrServe.
    TorrentClient(
        name = R.string.torrserve,
        url = createFDroidUrl(packageId = "ru.yourok.torrserve"),
        source = R.string.source_fdroid,
    ),
)

private data class TorrentClient(
    /* Name of the client. */
    @param:StringRes
    val name: Int,
    /** Where to get it. */
    val url: String,
    @param:StringRes
    /** Where it is available. */
    val source: Int,
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
    Column(modifier = modifier) {
        Text(stringResource(R.string.torrent_client_not_found_main_content))
        TorrentClientList()
    }
}

@Composable
private fun TorrentClientList() {
    val listContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val listContentColor = MaterialTheme.colorScheme.onSecondaryContainer

    ListSectionTitle(title = stringResource(R.string.section_title_recommended_client))
    TorrentClientLinkList(
        clients = recommendedClients,
        backgroundColor = listContainerColor,
        contentColor = listContentColor,
    )

    ListSectionTitle(title = stringResource(R.string.section_title_other_clients))
    TorrentClientLinkList(
        clients = otherClients,
        backgroundColor = listContainerColor.copy(alpha = 0.5f),
        contentColor = listContentColor,
    )
}

@Composable
private fun ListSectionTitle(title: String, modifier: Modifier = Modifier) {
    val color = AlertDialogDefaults.textContentColor.copy(alpha = 0.8f)

    Text(
        modifier = modifier.padding(vertical = 8.dp),
        text = title,
        color = color,
    )
}

@Composable
private fun TorrentClientLinkList(
    clients: List<TorrentClient>,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {

    val firstItemCornerShape = MaterialTheme.shapes.medium.copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp),
    )
    val lastItemCornerShape = MaterialTheme.shapes.medium.copy(
        topStart = CornerSize(0.dp),
        topEnd = CornerSize(0.dp),
    )

    clients.forEachIndexed { index, client ->
        val cornerShape = if (index == 0) {
            firstItemCornerShape
        } else if (clients.lastIndex == index) {
            lastItemCornerShape
        } else {
            null
        }

        TorrentClientLinkListItem(
            modifier = modifier,
            client = client,
            backgroundColor = backgroundColor,
            shape = cornerShape,
            contentColor = contentColor,
        )
    }
}

@Composable
private fun TorrentClientLinkListItem(
    client: TorrentClient,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
) {
    val uriHandler = LocalUriHandler.current

    val listItemColors = ListItemDefaults.colors(
        containerColor = backgroundColor,
        headlineColor = contentColor,
        trailingIconColor = contentColor,
    )

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape = shape ?: RectangleShape)
            .clickable { uriHandler.openUri(client.url) },
        headlineContent = {
            Text(
                text = stringResource(client.name),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(client.source),
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_open_in_new),
                contentDescription = null,
            )
        },
        colors = listItemColors,
    )
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