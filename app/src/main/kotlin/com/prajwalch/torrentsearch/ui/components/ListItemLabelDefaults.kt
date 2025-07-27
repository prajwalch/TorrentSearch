package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default values for the [ListItemLabelDefaults]. */
object ListItemLabelDefaults {
    /** Default shape for a list item label. */
    val shape: Shape
        @Composable get() = MaterialTheme.shapes.extraLarge

    /** Default container color for a list item label. */
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Default padding for a list item label. */
    val Padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

    /** Default space between leading icon and text. */
    val ContentSpacing = 4.dp

    /** Default size of a leading icon. */
    val IconSize = 16.dp
}

@Composable
fun ListItemLabel(
    @DrawableRes
    leadingIconId: Int,
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = ListItemLabelDefaults.shape,
    containerColor: Color = ListItemLabelDefaults.containerColor,
    contentPadding: PaddingValues = ListItemLabelDefaults.Padding,
    contentSpacing: Dp = ListItemLabelDefaults.ContentSpacing,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(space = contentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(size = ListItemLabelDefaults.IconSize),
            painter = painterResource(id = leadingIconId),
            contentDescription = null,
        )
        Text(text = text)
    }
}