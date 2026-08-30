package com.prajwalch.torrentsearch.ui.searchproviders.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.categoryStringResource
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.iconResId
import com.prajwalch.torrentsearch.ui.settings.searchproviders.SearchProviderProtection
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SearchProviderFilterRow(
    category: Category,
    onCategorySelect: (Category) -> Unit,
    protection: SearchProviderProtection?,
    onProtectionSelect: (SearchProviderProtection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCategoryDropdownMenu by rememberSaveable(category) {
        mutableStateOf(false)
    }
    var showProtectionDropdownMenu by rememberSaveable(protection) {
        mutableStateOf(false)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
    ) {
        Box {
            CategoryFilterChip(
                category = category,
                onClick = { showCategoryDropdownMenu = true },
            )
            CategoryDropdownMenu(
                expanded = showCategoryDropdownMenu,
                onDismiss = { showCategoryDropdownMenu = false },
                selectedCategory = category,
                onCategorySelect = onCategorySelect,
            )
        }

        Box {
            ProtectionFilterChip(
                protection = protection,
                onClick = { showProtectionDropdownMenu = true },
            )
            ProtectionDropdownMenu(
                expanded = showProtectionDropdownMenu,
                onDismiss = { showProtectionDropdownMenu = false },
                selectedProtection = protection,
                onProtectionSelect = onProtectionSelect,
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = category != Category.All
    val label = if (isSelected) {
        categoryStringResource(category)
    } else {
        stringResource(R.string.search_providers_chip_category)
    }

    FilterChip(
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                painter = painterResource(category.iconResId()),
                contentDescription = null,
            )
        },
        label = { Text(label) },
        trailingIcon = {
            Icon(
                modifier = modifier.size(FilterChipDefaults.IconSize),
                painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun CategoryDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedCategory: Category?,
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
                text = { Text(categoryStringResource(it)) },
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

@Composable
private fun ProtectionFilterChip(
    protection: SearchProviderProtection?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = protection != null
    val label = if (isSelected) {
        protection.displayName()
    } else {
        stringResource(R.string.search_providers_chip_protection)
    }
    val leadingIconResId = protection.iconResId()

    FilterChip(
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                painter = painterResource(leadingIconResId),
                contentDescription = null,
            )
        },
        label = { Text(label) },
        trailingIcon = {
            Icon(
                modifier = modifier.size(FilterChipDefaults.IconSize),
                painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun SearchProviderProtection.displayName(): String {
    val resId = when (this) {
        SearchProviderProtection.Protected -> R.string.search_providers_protection_protected
        SearchProviderProtection.Locked -> R.string.search_providers_protection_locked
        SearchProviderProtection.Unlocked -> R.string.search_providers_protection_unlocked
    }

    return stringResource(resId)
}

@DrawableRes
@Composable
private fun SearchProviderProtection?.iconResId(): Int = when (this) {
    SearchProviderProtection.Protected -> R.drawable.ic_shield
    SearchProviderProtection.Locked -> R.drawable.ic_shield_lock
    SearchProviderProtection.Unlocked -> R.drawable.ic_shield_checked
    null -> R.drawable.ic_shield
}

@Composable
private fun ProtectionDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedProtection: SearchProviderProtection?,
    onProtectionSelect: (SearchProviderProtection) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        SearchProviderProtection.entries.forEach {
            DropdownMenuItem(
                text = { Text(it.name) },
                onClick = { onProtectionSelect(it) },
                trailingIcon = {
                    if (it == selectedProtection) {
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