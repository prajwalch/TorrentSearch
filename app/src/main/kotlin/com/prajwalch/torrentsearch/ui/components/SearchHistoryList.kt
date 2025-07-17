package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryUiState

@Composable
fun SearchHistoryList(
    items: List<SearchHistoryUiState>,
    onSearchRequest: (String) -> Unit,
    onQueryChangeRequest: (String) -> Unit,
    onDeleteRequest: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(items = items, key = { it.id }) {
            SearchHistoryListItem(
                modifier = Modifier.animateItem(),
                query = it.query,
                onClick = { onSearchRequest(it.query) },
                onInsertClick = { onQueryChangeRequest(it.query) },
                onDeleteClick = { onDeleteRequest(it.id) },
            )
        }
    }
}


@Composable
private fun SearchHistoryListItem(
    query: String,
    onClick: () -> Unit,
    onInsertClick: () -> Unit,
    onDeleteClick: () -> Unit,
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
                IconButton(onClick = onInsertClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_insert),
                        contentDescription = null,
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                    )
                }
            }
        }
    )
}