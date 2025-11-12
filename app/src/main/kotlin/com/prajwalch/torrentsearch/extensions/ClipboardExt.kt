package com.prajwalch.torrentsearch.extensions

import android.content.ClipData

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

/** Copies the text into the clipboard. */
suspend fun Clipboard.copyText(text: String) {
    val clipData = ClipData.newPlainText(
        /* label = */
        null,
        /* text = */
        text,
    )
    val clipEntry = ClipEntry(clipData = clipData)

    this.setClipEntry(clipEntry = clipEntry)
}