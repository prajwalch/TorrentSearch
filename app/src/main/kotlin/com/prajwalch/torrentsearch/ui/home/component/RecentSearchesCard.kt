package com.prajwalch.torrentsearch.ui.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun RecentSearchesCard(
    queries: List<String>,
    onQueryClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spaces.small)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.spaces.large,
                        end = MaterialTheme.spaces.small,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_title_recent_searches),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )

                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                    )
                }
            }

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
        content = {
            Text(
                text = query,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}