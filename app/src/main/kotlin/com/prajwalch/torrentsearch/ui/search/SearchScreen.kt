package com.prajwalch.torrentsearch.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
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
    val viewModel = hiltViewModel<SearchViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExpandedSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = !showExpandedSearchBar,
                enter = fadeIn(),
                exit = fadeOut(),
                label = "Search screen top bar visibility animation",
            ) {
                SearchScreenTopBar(
                    onNavigateToBookmarks = onNavigateToBookmarks,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.focusable()) {
            SearchScreenContent(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .padding(innerPadding),
                query = uiState.query,
                onSearch = { onSearch(uiState.query, uiState.selectedCategory) },
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::changeCategory,
                onShowExpandedSearchBar = { showExpandedSearchBar = true },
            )

            ExpandedSearchBar(
                modifier = Modifier.zIndex(1f),
                query = uiState.query,
                onQueryChange = viewModel::changeQuery,
                onSearch = { onSearch(uiState.query, uiState.selectedCategory) },
                visible = showExpandedSearchBar,
                onVisibleChange = { showExpandedSearchBar = it },
            ) {
                SearchHistoryList(
                    histories = uiState.histories,
                    historyListItem = {
                        SearchHistoryListItem(
                            modifier = Modifier
                                .animateItem()
                                .clickable {
                                    onSearch(it.query, uiState.selectedCategory)
                                    showExpandedSearchBar = false
                                    viewModel.changeQuery(it.query)
                                },
                            query = it.query,
                            onInsertClick = { viewModel.changeQuery(it.query) },
                            onDeleteClick = { viewModel.deleteSearchHistory(it.id) },
                        )
                    },
                    key = { it.id },
                )
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

@Composable
private fun SearchScreenContent(
    query: String,
    onSearch: () -> Unit,
    categories: List<Category>,
    selectedCategory: Category,
    onCategorySelect: (Category) -> Unit,
    onShowExpandedSearchBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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

        // TODO: Don't use this search bar.
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spaces.large),
            query = query,
            onQueryChange = {},
            onSearch = {},
            expanded = false,
            onExpandChange = { onShowExpandedSearchBar() },
            content = {},
        )

        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelect = onCategorySelect,
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.spaces.large
            ),
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
        Button(
            onClick = onSearch,
            enabled = query.isNotBlank(),
        ) {
            Text(text = stringResource(R.string.button_search))
        }
    }
}

@Composable
private fun ExpandedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        label = "Expanded screen search bar visibility animation"
    ) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SearchBar(
            modifier = modifier.focusRequester(focusRequester),
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { if (query.isNotBlank()) onSearch() },
            expanded = true,
            onExpandChange = onVisibleChange,
            content = content,
        )
    }
}