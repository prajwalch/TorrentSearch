package com.prajwalch.torrentsearch.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.components.SearchBar
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.SearchHistoryListItem
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SearchScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSearch: (String, Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = activityScopedViewModel<SearchViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchScreenTopBar(
                onNavigateToBookmarks = onNavigateToBookmarks,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.large,
            ),
        ) {
            Text(
                modifier = Modifier.padding(vertical = MaterialTheme.spaces.extraLarge),
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )

            var searchBarExpanded by remember { mutableStateOf(false) }
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spaces.large),
                query = uiState.query,
                onQueryChange = viewModel::changeQuery,
                onSearch = {
                    if (uiState.query.isNotBlank()) {
                        onSearch(uiState.query, uiState.selectedCategory)
                    }
                },
                expanded = searchBarExpanded,
                onExpandChange = { searchBarExpanded = it },
            ) {
                SearchHistoryList(
                    histories = uiState.histories,
                    historyListItem = {
                        SearchHistoryListItem(
                            modifier = Modifier
                                .animateItem()
                                .clickable {
                                    viewModel.changeQuery(it.query)
                                    searchBarExpanded = false
                                    onSearch(uiState.query, uiState.selectedCategory)
                                },
                            query = it.query,
                            onInsertClick = { viewModel.changeQuery(it.query) },
                            onDeleteClick = { viewModel.deleteSearchHistory(it.id) },
                        )
                    },
                    key = { it.id },
                )
            }

            CategoryChipsRow(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::changeCategory,
                contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
            Button(
                onClick = { onSearch(uiState.query, uiState.selectedCategory) },
                enabled = uiState.query.isNotBlank(),
            ) {
                Text(text = stringResource(R.string.button_search))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenTopBar(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        actions = {
            IconButton(onClick = onNavigateToBookmarks) {
                Icon(
                    painter = painterResource(R.drawable.ic_star_filled),
                    contentDescription = null,
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
            }
        }
    )
}