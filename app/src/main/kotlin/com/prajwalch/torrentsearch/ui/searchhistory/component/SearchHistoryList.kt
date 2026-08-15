package com.prajwalch.torrentsearch.ui.searchhistory.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.SearchHistoriesByDate
import com.prajwalch.torrentsearch.data.repository.SearchHistoryDate
import com.prajwalch.torrentsearch.domain.model.SearchHistoryId
import com.prajwalch.torrentsearch.ui.theme.spaces

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Composable
fun SearchHistoryList(
    histories: SearchHistoriesByDate,
    onSearchRequest: (String) -> Unit,
    onCopyQueryToClipboard: (String) -> Unit,
    onDeleteSearchHistory: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        histories.forEach { (historyDate, histories) ->
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spaces.large,
                            vertical = MaterialTheme.spaces.small,
                        ),
                        text = historyDate.toGroupLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(items = histories, key = { it.id }) {
                SearchHistoryListItem(
                    modifier = Modifier
                        .combinedClickable(
                            interactionSource = null,
                            indication = LocalIndication.current,
                            onClick = { onSearchRequest(it.query) },
                            onLongClick = { onCopyQueryToClipboard(it.query) },
                        )
                        .animateItem(),
                    query = it.query,
                    onDeleteClick = { onDeleteSearchHistory(it.id) },
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryDate.toGroupLabel(): String {
    val mediumDateString = this.date.toMediumDateString()
    val gapInDays = this.date.until(LocalDate.now(), ChronoUnit.DAYS)

    return when (gapInDays) {
        0L -> "${stringResource(R.string.search_history_today)} - $mediumDateString"
        1L -> "${stringResource(R.string.search_history_yesterday)} - $mediumDateString"
        else -> mediumDateString
    }
}

private fun LocalDate.toMediumDateString(): String {
    val dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return this.format(dateTimeFormatter)
}

//@Composable
//fun SearchHistoryList(
//    histories: List<SearchHistory>,
//    onSearchRequest: (String) -> Unit,
//    onCopyQueryToClipboard: (String) -> Unit,
//    onDeleteSearchHistory: (SearchHistoryId) -> Unit,
//    modifier: Modifier = Modifier,
//    contentPadding: PaddingValues = PaddingValues(0.dp),
//) {
//    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
//        items(items = histories, key = { it.id }) {
//            SearchHistoryListItem(
//                modifier = Modifier
//                    .animateItem()
//                    .combinedClickable(
//                        interactionSource = null,
//                        indication = LocalIndication.current,
//                        onClick = { onSearchRequest(it.query) },
//                        onLongClick = { onCopyQueryToClipboard(it.query) },
//                    ),
//                query = it.query,
//                onDeleteClick = { onDeleteSearchHistory(it.id) },
//            )
//        }
//    }
//}
//

@Composable
private fun SearchHistoryListItem(
    query: String,
    onDeleteClick: () -> Unit,
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
            // Delete button.
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                )
            }
        },
    )
}