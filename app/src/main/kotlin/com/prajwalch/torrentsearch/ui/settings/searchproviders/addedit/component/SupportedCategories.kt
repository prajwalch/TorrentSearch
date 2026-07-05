package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SupportedCategories(
    supportedCategories: Set<Category>,
    onToggleCategorySelection: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allCategories = remember {
        Category.entries.filterNot { it == Category.All }
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        for (category in allCategories) {
            CategoryInputChip(
                category = category,
                selected = category in supportedCategories,
                onClick = { onToggleCategorySelection(category) },
            )
        }
    }
}