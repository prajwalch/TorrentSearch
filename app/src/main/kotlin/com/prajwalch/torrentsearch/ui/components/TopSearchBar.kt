package com.prajwalch.torrentsearch.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchBar(
    query: String,
    searchHistories: List<SearchHistoryUiState>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDeleteSearchHistory: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

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
                    expanded = expanded,
                    onQueryChange = onQueryChange,
                    onSearch = {
                        expanded = false
                        onSearch()
                    },
                    onExpandChange = { expanded = it },
                    onNavigateToSettings = onNavigateToSettings,
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            colors = SearchBarDefaults.colors(containerColor = containerColor),
        ) {
            SearchHistoryList(
                items = searchHistories,
                onSelectHistory = {
                    onQueryChange(it.query)
                    expanded = false
                    onSearch()
                },
                onInsertHistory = { onQueryChange(it.query) },
                onDeleteHistory = { onDeleteSearchHistory(it) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarInputField(
    query: String,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
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
            TrailingIcon(
                isQueryEmpty = query.isEmpty(),
                onNavigateToSettings = onNavigateToSettings,
                onClearQuery = { onQueryChange("") },
            )
        },
        interactionSource = interactionSource,
        colors = colors,
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
private fun TrailingIcon(
    isQueryEmpty: Boolean,
    onNavigateToSettings: () -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = !isQueryEmpty) {
            ClearQueryIconButton(onClick = onClearQuery)
        }
        SettingsIconButton(onClick = onNavigateToSettings)
    }
}

@Composable
private fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Settings,
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

@Composable
private fun SearchHistoryList(
    items: List<SearchHistoryUiState>,
    onSelectHistory: (SearchHistoryUiState) -> Unit,
    onInsertHistory: (SearchHistoryUiState) -> Unit,
    onDeleteHistory: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(items = items, key = { it.id }) {
            SearchHistoryListItem(
                modifier = Modifier.animateItem(),
                query = it.query,
                onClick = { onSelectHistory(it) },
                onInsert = { onInsertHistory(it) },
                onDelete = { onDeleteHistory(it.id) },
            )
        }
    }
}


@Composable
private fun SearchHistoryListItem(
    query: String,
    onClick: () -> Unit,
    onInsert: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            IconButton(onClick = onClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = null,
                )
            }
        },
        headlineContent = { Text(text = query) },
        trailingContent = {
            Row {
                IconButton(onClick = onInsert) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_insert),
                        contentDescription = null,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                    )
                }
            }
        }
    )
}