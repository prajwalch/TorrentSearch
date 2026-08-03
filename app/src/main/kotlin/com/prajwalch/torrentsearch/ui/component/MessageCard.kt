package com.prajwalch.torrentsearch.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

enum class MessageType {
    Tip,
    Info,
}

@Composable
fun MessageCard(
    onClose: () -> Unit,
    messageType: MessageType,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = messageType.containerColor(),
            contentColor = messageType.contentColor(),
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(messageType.iconRes()),
                    contentDescription = null,
                )
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyLarge
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        text()
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke()
                confirmButton?.invoke()
            }
        }
    }
}

@DrawableRes
@Composable
private fun MessageType.iconRes(): Int =
    when (this) {
        MessageType.Tip -> R.drawable.ic_lightbulb
        MessageType.Info -> R.drawable.ic_info
    }

@Composable
private fun MessageType.containerColor(): Color =
    when (this) {
        MessageType.Tip -> MaterialTheme.colorScheme.primaryContainer
        MessageType.Info -> MaterialTheme.colorScheme.secondaryContainer
    }

@Composable
private fun MessageType.contentColor(): Color =
    when (this) {
        MessageType.Tip -> MaterialTheme.colorScheme.onPrimaryContainer
        MessageType.Info -> MaterialTheme.colorScheme.onSecondaryContainer
    }

@Preview
@Composable
private fun TipMessageCardPreview() {
    TorrentSearchTheme(darkTheme = true) {
        MessageCard(
            onClose = {},
            messageType = MessageType.Tip,
            text = { Text("Swipe left to delete it") }
        )
    }
}