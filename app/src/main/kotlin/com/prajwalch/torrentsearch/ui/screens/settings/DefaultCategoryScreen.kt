package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun DefaultCategoryScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = activityScopedViewModel<SettingsViewModel>()
    val settings by viewModel.searchSettingsUiState.collectAsStateWithLifecycle()

    val defaultCategory by remember { derivedStateOf { settings.defaultCategory } }

    Scaffold(
        modifier = modifier,
        topBar = {
            DefaultCategoryScreenTopBar(onNavigateBack = onNavigateBack)
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(innerPadding),
            contentPadding = innerPadding,
        ) {
            items(items = Category.entries, contentType = { it }) { category ->
                CategoryListItem(
                    onClick = { viewModel.changeDefaultCategory(category) },
                    selected = category == defaultCategory,
                    name = category.name,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultCategoryScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.setting_default_category)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_to_settings_screen,
            )
        }
    )
}

@Composable
private fun CategoryListItem(
    onClick: () -> Unit,
    selected: Boolean,
    name: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        headlineContent = { Text(text = name) },
    )
}