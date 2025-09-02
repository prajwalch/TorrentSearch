package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R

@Composable
fun TextUrl(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            color = color,
            style = style,
        )
        Icon(
            modifier = Modifier.size(12.dp),
            painter = painterResource(R.drawable.ic_open_in_new),
            contentDescription = null,
        )
    }
}