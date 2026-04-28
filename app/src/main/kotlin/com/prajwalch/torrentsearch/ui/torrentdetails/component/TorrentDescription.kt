package com.prajwalch.torrentsearch.ui.torrentdetails.component

import android.text.util.Linkify
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle

import coil3.SingletonImageLoader
import com.prajwalch.torrentsearch.R
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun TorrentDescription(description: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    DetailsSection(
        modifier = modifier,
        title = { Text(stringResource(R.string.torrent_details_title_description)) },
    ) {
        if (description != null) {
            MarkdownText(
                markdown = description,
                linkColor = MaterialTheme.colorScheme.primary,
                isTextSelectable = true,
                textSelectionColors = LocalTextSelectionColors.current,
                imageLoader = SingletonImageLoader.get(context),
                linkifyMask = Linkify.WEB_URLS,
                enableSoftBreakAddsNewLine = true,
                syntaxHighlightColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                syntaxHighlightTextColor = MaterialTheme.colorScheme.onSurface,
                enableUnderlineForLink = true,
            )
        } else {
            Text(
                text = stringResource(R.string.torrent_details_message_not_available),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}