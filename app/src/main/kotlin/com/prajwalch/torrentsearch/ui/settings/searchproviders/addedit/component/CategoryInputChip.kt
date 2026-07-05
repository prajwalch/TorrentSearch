package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.iconResId

@Composable
fun CategoryInputChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    InputChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = { Text(categoryStringResource(category)) },
        leadingIcon = {
            val iconResId = if (selected) R.drawable.ic_check else category.iconResId()
            Icon(
                modifier = Modifier.size(InputChipDefaults.IconSize),
                painter = painterResource(iconResId),
                contentDescription = null,
            )
        },
    )
}