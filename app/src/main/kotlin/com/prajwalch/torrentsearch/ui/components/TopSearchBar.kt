package com.prajwalch.torrentsearch.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import com.prajwalch.torrentsearch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) {
    val unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val focusedContainerColor = MaterialTheme.colorScheme.surface

    val containerColor by animateColorAsState(
        if (expanded) {
            focusedContainerColor
        } else {
            unfocusedContainerColor
        }
    )

    Box(modifier = Modifier.semantics { isTraversalGroup = true }) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f }
                .then(modifier),
            inputField = {
                SearchBarInputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    expanded = expanded,
                    onExpandChange = onExpandChange,
                    onNavigateToBookmarks = onNavigateToBookmarks,
                    onNavigateToSettings = onNavigateToSettings,
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
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

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
        leadingIcon = {
            LeadingIcon(
                isFocused = isFocused,
                onBack = {
                    focusManager.clearFocus()
                    onExpandChange(false)
                }
            )
        },
        trailingIcon = {
            var showMoreMenu by remember { mutableStateOf(false) }

            Box {
                TrailingIcon(
                    isQueryEmpty = query.isEmpty(),
                    onClearQuery = { onQueryChange("") },
                    onMoreClick = { showMoreMenu = true },
                )
                MoreMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    onNavigateToBookmarks = onNavigateToBookmarks,
                    onNavigateToSettings = onNavigateToSettings,
                )

            }
        },
        interactionSource = interactionSource,
        colors = colors,
    )
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.medium,
    ) {
        DropdownMenuItem(
            onClick = {
                onDismissRequest()
                onNavigateToBookmarks()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_star_filled),
                    contentDescription = stringResource(R.string.button_go_to_bookmarks_screen),
                )
            },
            text = { Text(text = stringResource(R.string.bookmarks_screen_title)) },
        )
        DropdownMenuItem(
            onClick = {
                onDismissRequest()
                onNavigateToSettings()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.button_go_to_settings_screen),
                )
            },
            text = { Text(text = stringResource(R.string.settings_screen_title)) },
        )
    }
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
private fun TrailingIcon(
    isQueryEmpty: Boolean,
    onClearQuery: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = !isQueryEmpty) {
            ClearQueryIconButton(onClick = onClearQuery)
        }
        MoreIconButton(onClick = onMoreClick)
    }
}

@Composable
private fun MoreIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vertical),
            contentDescription = stringResource(R.string.button_go_to_settings_screen),
        )
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