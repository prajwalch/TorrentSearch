package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryId
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryUiState

@Composable
fun SearchHistoryList(
    histories: List<SearchHistoryUiState>,
    onSearchRequest: (String) -> Unit,
    onQueryChangeRequest: (String) -> Unit,
    onDeleteRequest: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(items = histories, key = { it.id }) {
            SearchHistoryListItem(
                modifier = Modifier
                    .animateItem()
                    .clickable { onSearchRequest(it.query) },
                query = it.query,
                onResearch = { onSearchRequest(it.query) },
                onInsertClick = { onQueryChangeRequest(it.query) },
                onDeleteClick = { onDeleteRequest(it.id) },
            )
        }
    }
}


@Composable
private fun SearchHistoryListItem(
    query: String,
    onResearch: () -> Unit,
    onInsertClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            IconButton(onClick = onResearch) {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = null,
                )
            }
        },
        headlineContent = { Text(text = query) },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onInsertClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_insert),
                        contentDescription = stringResource(R.string.button_select_search_history),
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.button_delete_search_history),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}