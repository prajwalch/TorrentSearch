package com.prajwalch.torrentsearch.ui.browse.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu

@Composable
fun CategoryDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedCategory: Category,
    onCategorySelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        Category.entries.forEach {
            DropdownMenuItem(
                text = { Text(text = categoryStringResource(it)) },
                onClick = { onCategorySelect(it) },
                trailingIcon = {
                    if (it == selectedCategory) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    }
}