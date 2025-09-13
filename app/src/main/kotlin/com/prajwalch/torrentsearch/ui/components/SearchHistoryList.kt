package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.search.SearchHistoryUiState

@Composable
fun SearchHistoryList(
    histories: List<SearchHistoryUiState>,
    historyListItem: @Composable LazyItemScope.(SearchHistoryUiState) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        items(items = histories, key = { it.id }) {
            historyListItem(it)
        }
    }
}

@Composable
fun SearchHistoryListItem(
    query: String,
    onInsertClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchHistoryListItem(
        modifier = modifier,
        query = query,
        actions = {
            InsertIconButton(onClick = onInsertClick)
            DeleteIconButton(onClick = onDeleteClick)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
fun SearchHistoryListItem(
    query: String,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchHistoryListItem(
        modifier = modifier,
        query = query,
        actions = { DeleteIconButton(onClick = onDeleteClick) },
    )
}

@Composable
private fun SearchHistoryListItem(
    query: String,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit) = {},
    colors: ListItemColors = ListItemDefaults.colors(),
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
            )
        },
        headlineContent = { Text(text = query) },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        },
        colors = colors,
    )
}

@Composable
private fun InsertIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_insert),
            contentDescription = stringResource(R.string.button_select_search_history),
        )
    }
}

@Composable
private fun DeleteIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.button_delete_search_history),
        )
    }
}