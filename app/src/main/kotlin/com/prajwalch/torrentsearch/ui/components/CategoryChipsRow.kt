package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category

@Composable
fun CategoryChipsRow(
    categories: List<Category>,
    selectedCategory: Category,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            items = categories,
            key = { it.ordinal },
            contentType = { it },
        ) { category ->
            CategoryChip(
                modifier = Modifier.animateItem(),
                label = category.toString(),
                selected = selectedCategory == category,
                isNSFW = category.isNSFW,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNSFW: Boolean = false,
) {
    val leadingIconSize = FilterChipDefaults.IconSize
    val border = FilterChipDefaults
        .filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (isNSFW) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        )

    val colors = if (isNSFW) {
        FilterChipDefaults.filterChipColors(
            labelColor = MaterialTheme.colorScheme.error,
            selectedContainerColor = MaterialTheme.colorScheme.error,
            selectedLabelColor = MaterialTheme.colorScheme.onError,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onError,
        )
    } else {
        FilterChipDefaults.filterChipColors()
    }

    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (selected) Icon(
                modifier = Modifier.size(leadingIconSize),
                imageVector = Icons.Outlined.Done,
                contentDescription = stringResource(R.string.desc_selected_category),
            )
        },
        border = border,
        colors = colors,
    )
}