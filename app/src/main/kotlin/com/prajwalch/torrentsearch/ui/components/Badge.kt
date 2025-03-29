package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Badge(
    text: String,
    icon: ImageVector? = null,
    color: Color = Color.White,
    background: Color = Color.DarkGray,
    fontSize: TextUnit = 14.sp
) {
    Row(
        modifier = Modifier
            .background(color = background, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(15.dp), tint = color)
            Spacer(Modifier.width(5.dp))
        }
        Text(text = text, color = color, fontSize = fontSize)
    }
}

@Preview(showBackground = true)
@Composable
fun BadgePreview() {
    Badge(text = "thepiratebay", icon = Icons.Default.Info)
}
