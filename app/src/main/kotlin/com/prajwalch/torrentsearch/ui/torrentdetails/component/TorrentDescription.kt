package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle

import com.prajwalch.torrentsearch.R

import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun TorrentDescription(description: String?, modifier: Modifier = Modifier) {
    SectionCard(
        modifier = modifier,
        title = stringResource(R.string.torrent_details_title_description),
    ) {
        if (description != null) {
            // TODO: Normalize HTML contained description.
            MarkdownText(
                markdown = description,
                linkColor = MaterialTheme.colorScheme.primary,
                enableSoftBreakAddsNewLine = true,
                syntaxHighlightColor = MaterialTheme.colorScheme.tertiaryContainer,
                syntaxHighlightTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                enableUnderlineForLink = true,
            )
//            SelectionContainer {
//                Text(text = AnnotatedString.fromHtml(description))
//            }
        } else {
            Text(
                text = stringResource(R.string.torrent_details_message_not_available),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}