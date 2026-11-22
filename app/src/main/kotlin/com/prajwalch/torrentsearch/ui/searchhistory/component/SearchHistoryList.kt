package com.prajwalch.torrentsearch.ui.searchhistory.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        histories.forEach { (historyDate, histories) ->
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = historyDate.toGroupLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                Spacer(Modifier.height(MaterialTheme.spaces.small))
            }

            itemsIndexed(items = histories, key = { _, history -> history.id }) { index, history ->
                SearchHistoryListItem(
                    modifier = Modifier.animateItem(),
                    query = history.query,
                    onClick = { onSearchRequest(history.query) },
                    onLongClick = { onCopyQueryToClipboard(history.query) },
                    onDeleteClick = { onDeleteSearchHistory(history.id) },
                    shapes = ListItemDefaults.segmentedShapes(index, histories.size),
                )
            }

            item {
                Spacer(Modifier.height(MaterialTheme.spaces.small))
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
) {
    SegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = shapes,
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
            )
        },
        content = { Text(text = query) },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                )
            }
        },
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
//    ListItem(
//        modifier = modifier,
//        leadingContent = {
//            Icon(
//                painter = painterResource(R.drawable.ic_history),
//                contentDescription = null,
//            )
//        },
//        content = { Text(text = query) },
//        trailingContent = {
//            IconButton(onClick = onDeleteClick) {
//                Icon(
//                    painter = painterResource(R.drawable.ic_delete),
//                    contentDescription = null,
//                )
//            }
//        },
//    )
}