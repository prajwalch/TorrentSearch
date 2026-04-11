package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R

@Composable
fun DetailsPageUrlPreview(
    url: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .border(
                width = 1.0.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .combinedClickable(
                interactionSource = null,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        leadingContent = {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_link),
                contentDescription = null,
            )
        },
        headlineContent = {
            Text(
                text = url,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
            headlineColor = MaterialTheme.colorScheme.primary,
        )
    )
}