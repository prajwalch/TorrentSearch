package com.prajwalch.torrentsearch.ui.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun RecentSearchesCard(
    queries: List<String>,
    onQueryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                text = stringResource(R.string.home_title_recent_searches),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )

            Column {
                queries.forEach { query ->
                    QueryListItem(
                        modifier = Modifier.clickable { onQueryClick(query) },
                        query = query,
                    )
                }
            }
        }
    }
}

@Composable
private fun QueryListItem(query: String, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
            )
        },
        headlineContent = {
            Text(
                text = query,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}