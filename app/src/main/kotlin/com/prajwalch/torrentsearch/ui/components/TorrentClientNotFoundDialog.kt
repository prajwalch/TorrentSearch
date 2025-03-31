package com.prajwalch.torrentsearch.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

@Composable
fun TorrentClientNotFoundDialog(onConfirmation: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        icon = { Icon(Icons.Default.Info, contentDescription = "Example Icon") },
        title = { Text("Torrent client not found") },
        text = { Text(dialogContent(uriHandler)) },
        onDismissRequest = { onConfirmation() },
        confirmButton = { TextButton(onClick = onConfirmation) { Text("Done") } },
    )
}

private fun dialogContent(uriHandler: UriHandler): AnnotatedString {
    val flud = clientPlayStoreLink(
        "https://play.google.com/store/apps/details?id=com.delphicoder.flud&hl=en",
        uriHandler
    )
    val libreTorrent = clientPlayStoreLink(
        "https://play.google.com/store/apps/details?id=org.proninyaroslav.libretorrent",
        uriHandler
    )

    return buildAnnotatedString {
        append("Download functionality requires an external torrent client to be installed. ")
        append("Install any one of the client from the given list of clients.\n")
        append('\n')
        withLink(flud) { append("Flud (Recommended)\n") }
        withLink(libreTorrent) { append("LibreTorrent") }
    }
}

private fun clientPlayStoreLink(url: String, uriHandler: UriHandler): LinkAnnotation.Url {
    return LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = Color.Cyan))) {
        val url = (it as LinkAnnotation.Url).url
        uriHandler.openUri(url)
    }
}
