package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prajwalch.torrentsearch.R

@Composable
fun TorrentClientNotFoundDialog(onConfirmation: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        modifier = modifier,
        icon = { DialogLeadingIcon() },
        title = { DialogTitle() },
        text = { DialogContent() },
        onDismissRequest = onConfirmation,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(stringResource(R.string.button_done))
            }
        },
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.torrent_client_not_found_main_content))
        ClientList()
    }
}

@Composable
private fun ClientList(modifier: Modifier = Modifier) {
    // Stackoverflow help:
    // https://stackoverflow.com/questions/67605986/add-icon-at-last-word-of-text-in-jetpack-compose

    // 1. Create links and then create text content.
    val links = listOf(
        // (link, label)
        Pair(
            buildPlayStoreLink(packageId = "com.delphicoder.flud"),
            stringResource(R.string.flud_client_link_label)
        ),
        Pair(
            buildPlayStoreLink(packageId = "org.proninyaroslav.libretorrent"),
            stringResource(R.string.libretorrent_client_link_label)
        )
    )
    // <url>[placeholder]
    val linkIndicatorPlaceholder = "link_indicator"
    // Content which contains links and link indicator placeholder for each of them.
    //
    // We need to replace the each placeholder with the actual icon.
    // <url>[placeholder] -> <url>[actual icon]
    val content = buildLinkList(
        links = links,
        linkIndicatorPlaceholder = { linkIndicatorPlaceholder }
    )

    // 2. Define replacement content for link indicator.
    val linkIndicatorSize = 12.sp
    val linkIndicatorPlaceholderReplacement = InlineTextContent(
        // Tells the sizing and layout of the placeholder in which the
        // content will be placed.
        placeholder = Placeholder(
            width = linkIndicatorSize,
            height = linkIndicatorSize,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        // Replacement content.
        Icon(
            painter = painterResource(R.drawable.ic_open_in_new),
            contentDescription = null,
        )
    }

    // 3. Give content and placeholder replacements to `Text`.
    //
    // List of (placeholder, replacement). Only one in our case.
    //
    // We need to pass all the placeholder and their replacement, present in
    // the content to `Text` so that it can replace all of them.
    val placeholderReplacements = mapOf(
        Pair(
            linkIndicatorPlaceholder,
            linkIndicatorPlaceholderReplacement
        )
    )

    Text(modifier = modifier, text = content, inlineContent = placeholderReplacements)
}

private fun buildPlayStoreLink(packageId: String): LinkAnnotation.Url {
    val url = "https://play.google.com/store/apps/details?id=$packageId"

    val urlColor = Color(0xFF2196F3)
    val urlStyle = TextLinkStyles(
        style = SpanStyle(color = urlColor)
    )

    return LinkAnnotation.Url(url = url, styles = urlStyle)
}

private fun buildLinkList(
    links: List<Pair<LinkAnnotation.Url, String>>,
    linkIndicatorPlaceholder: () -> String,
): AnnotatedString {
    val bullet = "\u2022"

    return buildAnnotatedString {
        for ((link, label) in links) {
            append(bullet)
            withLink(link = link) {
                append(text = label)
                append(' ')
                // Link indicator placeholder. We will replace it later.
                appendInlineContent(
                    id = linkIndicatorPlaceholder(),
                    alternateText = "[link_indicator_icon]"
                )
            }
            appendLine()
        }
    }
}