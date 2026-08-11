package com.prajwalch.torrentsearch.ui.torrentdetails.component

import android.text.util.Linkify

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import coil3.SingletonImageLoader
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun TorrentDescription(description: String, modifier: Modifier = Modifier) {
    MarkdownText(
        modifier = modifier,
        markdown = description,
        linkColor = MaterialTheme.colorScheme.primary,
        isTextSelectable = true,
        textSelectionColors = LocalTextSelectionColors.current,
        imageLoader = SingletonImageLoader.get(LocalContext.current),
        linkifyMask = Linkify.WEB_URLS,
        enableSoftBreakAddsNewLine = true,
        syntaxHighlightColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        syntaxHighlightTextColor = MaterialTheme.colorScheme.onSurface,
        enableUnderlineForLink = true,
    )
}