package com.prajwalch.torrentsearch.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex

import com.prajwalch.torrentsearch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit),
) {
    val unfocusedContainerColor = lerp(
        start = MaterialTheme.colorScheme.secondaryContainer,
        stop = MaterialTheme.colorScheme.surface,
        fraction = 0.4f,
    )
    val focusedContainerColor = MaterialTheme.colorScheme.surface

    val containerColor by animateColorAsState(
        if (expanded) focusedContainerColor else unfocusedContainerColor
    )

    Box(modifier = Modifier.semantics { isTraversalGroup = true }) {
        SearchBar(
            modifier = modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarInputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    expanded = expanded,
                    onExpandChange = onExpandChange,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                )
            },
            expanded = expanded,
            onExpandedChange = onExpandChange,
            colors = SearchBarDefaults.colors(containerColor = containerColor),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val unfocusedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val focusedContentColor = MaterialTheme.colorScheme.onSurface

    val colors = SearchBarDefaults.inputFieldColors(
        unfocusedTextColor = unfocusedContentColor,
        unfocusedPlaceholderColor = unfocusedContentColor,
        unfocusedLeadingIconColor = unfocusedContentColor,
        unfocusedTrailingIconColor = unfocusedContentColor,

        focusedTextColor = focusedContentColor,
        focusedPlaceholderColor = focusedContentColor,
        focusedLeadingIconColor = focusedContentColor,
        focusedTrailingIconColor = focusedContentColor,
    )

    SearchBarDefaults.InputField(
        modifier = modifier,
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { onSearch() },
        expanded = expanded,
        onExpandedChange = onExpandChange,
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = leadingIcon ?: {
            LeadingIcon(
                isFocused = isFocused,
                onBack = { onExpandChange(false) },
            )
        },
        trailingIcon = trailingIcon ?: {
            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.desc_clear_search_query),
                    )
                }
            }
        },
        interactionSource = interactionSource,
        colors = colors,
    )
}


@Composable
private fun LeadingIcon(isFocused: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedContent(modifier = modifier, targetState = isFocused) { focused ->
        if (focused) {
            NavigateBackIconButton(
                onClick = onBack,
                contentDescriptionId = R.string.desc_unfocus_search_bar,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        }
    }
}