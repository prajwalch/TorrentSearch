package com.prajwalch.torrentsearch.ui.settings.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.R

@Composable
fun ExpandableItem(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    @DrawableRes icon: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val trailingIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable(onClick = onToggle),
            leadingContent = icon?.let { iconId ->
                {
                    Icon(
                        painter = painterResource(iconId),
                        contentDescription = null,
                    )
                }
            },
            headlineContent = { Text(title) },
            trailingContent = {
                Icon(
                    modifier = Modifier.rotate(trailingIconRotation),
                    painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                    contentDescription = null,
                )
            },
            supportingContent = { Text(subtitle) },
        )

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}