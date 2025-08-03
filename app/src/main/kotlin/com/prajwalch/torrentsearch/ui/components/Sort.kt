package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.SortCriteria
import com.prajwalch.torrentsearch.data.SortOrder

@Composable
fun SortButtonAndMenu(
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortRequest: (SortCriteria, SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember(currentSortCriteria) { mutableStateOf(false) }

    Box(modifier = modifier) {
        SortButton(
            onClick = { showMenu = true },
            currentSortCriteria = currentSortCriteria,
            currentSortOrder = currentSortOrder,
            onSortOrderChange = { onSortRequest(currentSortCriteria, it) },
        )
        SortOptionsMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            selectedSortCriteria = currentSortCriteria,
            onSortCriteriaChange = { onSortRequest(it, currentSortOrder) },
        )
    }
}


@Composable
fun SortButton(
    onClick: () -> Unit,
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Companion.CenterVertically,
    ) {
        SortCriteriaButton(
            onClick = onClick,
            currentCriteria = currentSortCriteria,
        )
        SortOrderIconButton(
            onClick = { onSortOrderChange(currentSortOrder.opposite()) },
            currentOrder = currentSortOrder,
        )
    }
}

@Composable
private fun SortCriteriaButton(
    onClick: () -> Unit,
    currentCriteria: SortCriteria,
    modifier: Modifier = Modifier,
) {
    TextButton(modifier = modifier, onClick = onClick) {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            painter = painterResource(R.drawable.ic_sort),
            contentDescription = stringResource(R.string.button_open_sort_options),
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = currentCriteria.toString(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SortOrderIconButton(
    onClick: () -> Unit,
    currentOrder: SortOrder,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val icon = when (currentOrder) {
        SortOrder.Ascending -> R.drawable.ic_arrow_up
        SortOrder.Descending -> R.drawable.ic_arrow_down
    }

    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            painter = painterResource(icon),
            contentDescription = stringResource(
                R.string.button_change_sort_order,
                currentOrder.opposite(),
            ),
            tint = tint,
        )
    }
}

@Composable
fun SortOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedSortCriteria: SortCriteria,
    onSortCriteriaChange: (SortCriteria) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.medium,
    ) {
        for (sortItem in SortCriteria.entries) {
            DropdownMenuItem(
                text = { Text(text = sortItem.toString()) },
                onClick = { onSortCriteriaChange(sortItem) },
                trailingIcon = {
                    RadioButton(
                        selected = sortItem == selectedSortCriteria,
                        onClick = { onSortCriteriaChange(sortItem) },
                    )
                },
                contentPadding = PaddingValues(start = 16.dp, end = 4.dp),
            )
        }
    }
}