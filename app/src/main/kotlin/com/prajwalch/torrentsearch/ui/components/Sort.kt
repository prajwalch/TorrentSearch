package com.prajwalch.torrentsearch.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.viewmodel.SortKey
import com.prajwalch.torrentsearch.ui.viewmodel.SortOrder

@Composable
fun SortButton(
    currentSortKey: SortKey,
    onClick: () -> Unit,
    currentSortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Companion.CenterVertically,
    ) {
        SortKeyButton(
            currentKey = currentSortKey,
            onClick = onClick,
        )
        SortOrderIconButton(
            currentOrder = currentSortOrder,
            onClick = { onSortOrderChange(currentSortOrder.opposite()) }
        )
    }
}

@Composable
private fun SortKeyButton(
    currentKey: SortKey,
    onClick: () -> Unit,
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
            text = currentKey.toString(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SortOrderIconButton(
    currentOrder: SortOrder,
    onClick: () -> Unit,
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
    selectedKey: SortKey,
    onSortKeySelect: (SortKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.medium,
    ) {
        for (sortItem in SortKey.entries) {
            DropdownMenuItem(
                text = { Text(text = sortItem.toString()) },
                onClick = { onSortKeySelect(sortItem) },
                leadingIcon = {
                    RadioButton(
                        selected = sortItem == selectedKey,
                        onClick = { onSortKeySelect(sortItem) },
                    )
                }
            )
        }
    }
}