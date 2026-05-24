package com.prajwalch.torrentsearch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.ui.theme.spaces

object ContentStateDefaults {
    val IconSize: Dp = 80.dp

    val SmallIconSize: Dp = 48.dp

    val TitleTextStyle: TextStyle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

    val DescriptionTextStyle: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium
}

@Composable
fun ContentState(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    description: @Composable (() -> Unit)? = null,
    primaryAction: @Composable (() -> Unit)? = null,
    secondaryAction: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.large,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.let { it() }

        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.extraSmall,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompositionLocalProvider(
                value = LocalTextStyle provides ContentStateDefaults.TitleTextStyle,
                content = title,
            )
            CompositionLocalProvider(
                LocalTextStyle provides ContentStateDefaults.DescriptionTextStyle
            ) {
                description?.let { it() }
            }
        }

        Row(
            modifier = Modifier.padding(vertical = MaterialTheme.spaces.extraLarge),
            horizontalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.large,
                alignment = Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            secondaryAction?.let { it() }
            primaryAction?.let { it() }
        }
    }
}