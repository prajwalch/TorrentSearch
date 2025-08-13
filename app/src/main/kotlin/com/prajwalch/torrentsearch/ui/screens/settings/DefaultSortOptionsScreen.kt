package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.repository.SortCriteria
import com.prajwalch.torrentsearch.data.repository.SortOrder
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun DefaultSortOptionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val searchSettings by viewModel.searchSettingsUiState.collectAsStateWithLifecycle()
    val defaultSortOptions by remember { derivedStateOf { searchSettings.defaultSortOptions } }

    Scaffold(
        modifier = modifier,
        topBar = {
            DefaultSortOptionsScreenTopBar(onNavigateBack = onNavigateBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(innerPadding),
            contentPadding = innerPadding,
        ) {
            item {
                SortCriteriaSection(
                    selectedCriteria = defaultSortOptions.sortCriteria,
                    onCriteriaSelect = viewModel::changeDefaultSortCriteria,
                )
            }

            item {
                SortOrderSection(
                    selectedOrder = defaultSortOptions.sortOrder,
                    onOrderSelect = viewModel::changeDefaultSortOrder,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultSortOptionsScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.setting_default_sort_options)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_to_settings_screen,
            )
        },
    )
}

@Composable
private fun SortCriteriaSection(
    selectedCriteria: SortCriteria,
    onCriteriaSelect: (SortCriteria) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_sort_criteria)

        for (criteria in SortCriteria.entries) {
            ListItem(
                modifier = Modifier
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onCriteriaSelect(criteria) },
                    ),
                leadingContent = {
                    RadioButton(
                        selected = criteria == selectedCriteria,
                        onClick = { onCriteriaSelect(criteria) },
                    )
                },
                headlineContent = { Text(text = criteria.toString()) }
            )
        }
    }
}

@Composable
private fun SortOrderSection(
    selectedOrder: SortOrder,
    onOrderSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsSectionTitle(titleId = R.string.settings_section_sort_order)

        for (order in SortOrder.entries) {
            ListItem(
                modifier = Modifier
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onOrderSelect(order) },
                    ),
                leadingContent = {
                    RadioButton(
                        selected = order == selectedOrder,
                        onClick = { onOrderSelect(order) },
                    )
                },
                headlineContent = { Text(text = order.toString()) }
            )
        }
    }
}