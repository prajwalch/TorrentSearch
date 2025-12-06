package com.prajwalch.torrentsearch.ui.settings.defaultsortoptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.settings.SettingsViewModel
import com.prajwalch.torrentsearch.utils.sortCriteriaStringResource
import com.prajwalch.torrentsearch.utils.sortOrderStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultSortOptionsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val searchSettings by viewModel.searchSettingsUiState.collectAsStateWithLifecycle()

    val defaultSortOptions by remember { derivedStateOf { searchSettings.defaultSortOptions } }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(connection = scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            DefaultSortOptionsScreenTopBar(
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding),
        ) {
            SortCriteriaSection(
                selectedCriteria = defaultSortOptions.criteria,
                onCriteriaSelect = viewModel::setDefaultSortCriteria,
            )
            SortOrderSection(
                selectedOrder = defaultSortOptions.order,
                onOrderSelect = viewModel::setDefaultSortOrder,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultSortOptionsScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.settings_default_sort_options)) },
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SortCriteriaSection(
    selectedCriteria: SortCriteria,
    onCriteriaSelect: (SortCriteria) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsSectionTitle(title = R.string.settings_section_sort_criteria)

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
                headlineContent = { Text(text = sortCriteriaStringResource(criteria)) },
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
        SettingsSectionTitle(title = R.string.settings_section_sort_order)

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
                headlineContent = { Text(text = sortOrderStringResource(order)) },
            )
        }
    }
}