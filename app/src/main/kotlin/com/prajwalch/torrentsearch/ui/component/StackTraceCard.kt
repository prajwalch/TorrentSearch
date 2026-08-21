package com.prajwalch.torrentsearch.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun StackTraceCard(
    stackTrace: String,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.spaces.large),
) {
    var stackTraceCopied by rememberSaveable(stackTrace) { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    LaunchedEffect(stackTraceCopied) {
        if (stackTraceCopied) {
            delay(2.seconds)
            stackTraceCopied = false
        }
    }

    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
    ) {
        Box(Modifier.padding(contentPadding)) {
            Text(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                text = stackTrace,
                fontFamily = FontFamily.Monospace,
            )

            CopyButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = {
                    coroutineScope.launch {
                        clipboard.copyText(stackTrace)
                        stackTraceCopied = true
                    }
                },
                copied = stackTraceCopied,
            )
        }
    }
}

@Composable
private fun CopyButton(
    onClick: () -> Unit,
    copied: Boolean,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        modifier = modifier,
        onClick = onClick,
        enabled = !copied,
    ) {
        Crossfade(copied) { isTextCopied ->
            val iconResId = if (isTextCopied) R.drawable.ic_check else R.drawable.ic_copy

            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun StackTraceCardPreview() {
    TorrentSearchTheme {
        StackTraceCard(
            modifier = Modifier.height(360.dp),
            stackTrace = IllegalArgumentException().stackTraceToString(),
        )
    }
}