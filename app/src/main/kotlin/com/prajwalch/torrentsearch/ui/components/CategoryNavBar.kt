package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.data.Category

@Composable
fun CategoryNavBar(activeCategory: Category, onSelect: (Category) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(Category.entries.toList()) { index, category ->
            // TODO: This is hack to add space between items.
            //       horizontalArrangement is not working.
            if (index != 0) {
                Spacer(Modifier.width(8.dp))
            }

            CategoryNavBarItem(
                label = category.toString(),
                isActive = activeCategory == category,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
private fun CategoryNavBarItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (isActive) Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Selected category",
                modifier = Modifier.Companion.size(FilterChipDefaults.IconSize)
            )
        },
    )
}