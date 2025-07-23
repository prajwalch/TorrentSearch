package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.ui.components.DialogListItem
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.components.SettingsItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle

@Composable
fun GeneralSettings(modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.generalSettingsUiState.collectAsStateWithLifecycle()

    var showCategoryListDialog by remember(settings) { mutableStateOf(false) }

    if (showCategoryListDialog) {
        CategoryListDialog(
            onDismissRequest = { showCategoryListDialog = false },
            defaultCategory = settings.defaultCategory,
            onDefaultCategoryChange = { viewModel.updateDefaultCategory(it) },
        )
    }

    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_general)
        SettingsItem(
            onClick = { showCategoryListDialog = true },
            leadingIconId = R.drawable.ic_category_search,
            headlineId = R.string.setting_default_category,
            supportingContent = settings.defaultCategory.name,
        )
        SettingsItem(
            onClick = { viewModel.updateEnableNSFWMode(!settings.enableNSFWMode) },
            leadingIconId = R.drawable.ic_18_up_rating,
            headlineId = R.string.setting_enable_nsfw_mode,
            trailingContent = {
                Switch(
                    checked = settings.enableNSFWMode,
                    onCheckedChange = { viewModel.updateEnableNSFWMode(it) },
                )
            },
        )
    }
}

@Composable
private fun CategoryListDialog(
    onDismissRequest: () -> Unit,
    defaultCategory: Category,
    onDefaultCategoryChange: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        titleId = R.string.setting_default_category,
    ) {
        LazyColumn {
            items(
                items = Category.entries,
                contentType = { it },
            ) { category ->
                CategoryListItem(
                    selected = category == defaultCategory,
                    onClick = { onDefaultCategoryChange(category) },
                    name = category.name,
                )
            }
        }
    }
}

@Composable
private fun CategoryListItem(
    selected: Boolean,
    onClick: () -> Unit,
    name: String,
    modifier: Modifier = Modifier,
) {
    DialogListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        headlineContent = { Text(text = name) },
    )
}