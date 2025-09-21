package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder

@Composable
fun SortMenu(
    currentSortCriteria: SortCriteria,
    currentSortOrder: SortOrder,
    onSortRequest: (SortCriteria, SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSortCriteriaDropdownMenu by remember(currentSortCriteria) {
        mutableStateOf(false)
    }

    Box(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SortCriteriaButton(
                onClick = { showSortCriteriaDropdownMenu = true },
                currentCriteria = currentSortCriteria,
            )
            SortOrderButton(
                onClick = {
                    onSortRequest(currentSortCriteria, currentSortOrder.opposite())
                },
                currentOrder = currentSortOrder,
            )
        }
        SortCriteriaDropdownMenu(
            expanded = showSortCriteriaDropdownMenu,
            onDismissRequest = { showSortCriteriaDropdownMenu = false },
            selectedSortCriteria = currentSortCriteria,
            onSortCriteriaChange = { onSortRequest(it, currentSortOrder) },
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
private fun SortOrderButton(
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
private fun SortCriteriaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedSortCriteria: SortCriteria,
    onSortCriteriaChange: (SortCriteria) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        for (sortItem in SortCriteria.entries) {
            DropdownMenuItem(
                text = { Text(text = sortItem.toString()) },
                onClick = { onSortCriteriaChange(sortItem) },
                trailingIcon = {
                    if (sortItem == selectedSortCriteria) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    }
}