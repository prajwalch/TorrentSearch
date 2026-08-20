package com.prajwalch.torrentsearch.ui.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R

@Composable
fun SearchSuggestionList(
    queries: List<String>,
    onSearchRequest: (String) -> Unit,
    onInsertQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        items(items = queries, key = { it }) {
            SearchSuggestionListItem(
                modifier = Modifier
                    .animateItem()
                    .clickable { onSearchRequest(it) },
                query = it,
                onInsertClick = { onInsertQuery(it) },
            )
        }
    }
}

@Composable
private fun SearchSuggestionListItem(
    query: String,
    onInsertClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            IconButton(onClick = onInsertClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_insert),
                    contentDescription = null,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}