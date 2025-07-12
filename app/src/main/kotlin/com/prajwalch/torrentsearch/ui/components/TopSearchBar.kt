package com.prajwalch.torrentsearch.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.prajwalch.torrentsearch.R

@Composable
fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = TextFieldDefaults.shape,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusManager = LocalFocusManager.current

    val unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val unfocusedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val focusedContentColor = MaterialTheme.colorScheme.onSurface
    val indicatorColor = Color.Transparent

    val colors = TextFieldDefaults.colors(
        unfocusedContainerColor = unfocusedContainerColor,
        unfocusedTextColor = unfocusedContentColor,
        unfocusedPlaceholderColor = unfocusedContentColor,
        unfocusedLeadingIconColor = unfocusedContentColor,
        unfocusedTrailingIconColor = unfocusedContentColor,
        unfocusedIndicatorColor = indicatorColor,

        focusedContainerColor = focusedContainerColor,
        focusedTextColor = focusedContentColor,
        focusedPlaceholderColor = focusedContentColor,
        focusedLeadingIconColor = focusedContentColor,
        focusedTrailingIconColor = focusedContentColor,
        focusedIndicatorColor = indicatorColor,
    )

    TextField(
        modifier = modifier.clip(shape = shape),
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = {
            LeadingIcon(
                isFocused = isFocused,
                onBack = { focusManager.clearFocus() }
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
                label = "Clear query button visibility animation"
            ) {
                ClearQueryIconButton(onClick = { onQueryChange("") })
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = colors,
        interactionSource = interactionSource,
        shape = shape,
    )
}

@Composable
private fun LeadingIcon(isFocused: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = isFocused) { focused ->
        if (focused) {
            IconButton(onClick = onBack) {
                Icon(
                    modifier = modifier,
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.desc_unfocus_search_bar),
                )
            }
        } else {
            Icon(
                modifier = modifier,
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ClearQueryIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Clear,
            contentDescription = stringResource(R.string.desc_clear_search_query)
        )
    }
}