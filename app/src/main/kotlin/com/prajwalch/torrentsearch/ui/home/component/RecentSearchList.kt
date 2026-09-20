package com.prajwalch.torrentsearch.ui.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

import com.prajwalch.torrentsearch.R

@Composable
fun RecentSearchList(
    queries: List<String>,
    onQueryClick: (String) -> Unit,
    onInsertQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        queries.forEach {
            RecentSearchListItem(
                modifier = Modifier.clickable { onQueryClick(it) },
                query = it,
                onInsertQuery = { onInsertQuery(it) },
            )
        }
    }
}

@Composable
private fun RecentSearchListItem(
    query: String,
    onInsertQuery: () -> Unit,
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
        headlineContent = { Text(query) },
        trailingContent = {
            IconButton(onClick = onInsertQuery) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_insert),
                    contentDescription = null,
                )
            }
        },
    )
}

@Preview
@Composable
private fun RecentSearchListPreview() {
    RecentSearchList(
        queries = listOf(
            "One",
            "Spider Man",
            "Avengers: Doomsday",
            "Avengers",
            "Primer",
            "Predestination",
            "Looper",
            "The hunting house on the hill",
        ),
        onQueryClick = {},
        onInsertQuery = {},
    )
}