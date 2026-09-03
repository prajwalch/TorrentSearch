package com.prajwalch.torrentsearch.ui.settings.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

private object DarkThemeOptionDefaults {
    val Height: Dp = 80.dp

    val Shape: Shape
        @Composable get() = MaterialTheme.shapes.medium

    val SelectedColors: DarkThemeOptionColors
        @Composable get() = DarkThemeOptionColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            borderColor = MaterialTheme.colorScheme.primary,
        )

    val DefaultColors: DarkThemeOptionColors
        @Composable get() = DarkThemeOptionColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        )

    val IconSize: Dp = 28.dp

    @Composable
    fun colors(selected: Boolean): DarkThemeOptionColors {
        return if (selected) SelectedColors else DefaultColors
    }
}

private data class DarkThemeOptionColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
)

@Composable
fun DarkThemeOption(
    onClick: () -> Unit,
    selected: Boolean,
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = DarkThemeOptionDefaults.colors(selected)

    Surface(
        modifier = modifier
            .height(DarkThemeOptionDefaults.Height)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            },
        selected = selected,
        onClick = onClick,
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shape = DarkThemeOptionDefaults.Shape,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = colors.borderColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spaces.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.extraSmall,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Icon(
                modifier = Modifier.size(DarkThemeOptionDefaults.IconSize),
                painter = icon,
                contentDescription = null,
                tint = if (selected) {
                    LocalContentColor.current
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )

            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun DarkThemeOptionPreview() {
    TorrentSearchTheme {
        DarkThemeOption(
            onClick = {},
            selected = true,
            icon = painterResource(R.drawable.ic_dark_mode),
            label = "On",
        )
    }
}