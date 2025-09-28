package com.prajwalch.torrentsearch.ui.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder

@Composable
fun SortIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_sort),
            contentDescription = stringResource(R.string.button_open_sort_options),
        )
    }
}

@Composable
fun SortDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortRequest: (SortCriteria, SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        for (criteria in SortCriteria.entries) {
            DropdownMenuItem(
                text = { Text(text = criteria.toString()) },
                onClick = { onSortRequest(criteria, currentSortOrder) },
                trailingIcon = { if (criteria == currentSortCriteria) CheckIcon() },
            )
        }

        HorizontalDivider()

        for (order in SortOrder.entries) {
            DropdownMenuItem(
                text = { Text(text = order.toString()) },
                onClick = { onSortRequest(currentSortCriteria, order) },
                trailingIcon = { if (order == currentSortOrder) CheckIcon() },
            )
        }
    }
}

@Composable
private fun CheckIcon(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier,
        painter = painterResource(R.drawable.ic_check),
        contentDescription = null,
    )
}