package com.prajwalch.torrentsearch.ui.search

import android.os.Build

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.database.entities.SearchHistoryId
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.ui.components.SearchBar
import com.prajwalch.torrentsearch.ui.components.SearchHistoryList
import com.prajwalch.torrentsearch.ui.components.SearchHistoryListItem
import com.prajwalch.torrentsearch.ui.components.SettingsIconButton
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSearch: (String, Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SearchViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(connection = scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            SearchScreenTopBar(
                onNavigateToBookmarks = onNavigateToBookmarks,
                onNavigateToSettings = onNavigateToSettings,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        // Prevent search bar from being auto focused on older Android versions
        // which range from 7.1 to 8.1 (<9).
        val focusableModifier = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Modifier.focusable()
        } else {
            Modifier
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding)
                .padding(
                    horizontal = MaterialTheme.spaces.large,
                    vertical = MaterialTheme.spaces.extraLarge,
                )
                .then(focusableModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.spaces.large,
            ),
        ) {
            val coroutineScope = rememberCoroutineScope()
            val searchBarState = rememberSearchBarState()
            val textFieldState = rememberTextFieldState()

            val enableSearchButton by remember {
                derivedStateOf { textFieldState.text.isNotBlank() }
            }

            SearchBar(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = {
                    onSearch(textFieldState.text.toString(), uiState.selectedCategory)
                },
            ) {
                SearchHistoryList(
                    histories = uiState.histories,
                    onSearchQueryClick = {
                        onSearch(it, uiState.selectedCategory)
                        textFieldState.setTextAndPlaceCursorAtEnd(it)
                        coroutineScope.launch { searchBarState.animateToCollapsed() }
                    },
                    onChangeSearchQuery = textFieldState::setTextAndPlaceCursorAtEnd,
                    onDeleteSearchHistory = viewModel::deleteSearchHistory,
                )
            }

            CategoryChipsRow(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategoryClick = viewModel::setCategory,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSearch(textFieldState.text.toString(), uiState.selectedCategory)
                },
                enabled = enableSearchButton,
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
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeTopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = onNavigateToBookmarks) {
                Icon(
                    painter = painterResource(R.drawable.ic_star_filled),
                    contentDescription = null,
                )
            }
            SettingsIconButton(onClick = onNavigateToSettings)
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SearchHistoryList(
    histories: List<SearchHistoryItemUiState>,
    onSearchQueryClick: (String) -> Unit,
    onChangeSearchQuery: (String) -> Unit,
    onDeleteSearchHistory: (SearchHistoryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchHistoryList(
        modifier = modifier,
        histories = histories,
        historyListItem = {
            SearchHistoryListItem(
                modifier = Modifier
                    .animateItem()
                    .clickable(onClick = { onSearchQueryClick(it.query) }),
                query = it.query,
                onInsertClick = { onChangeSearchQuery(it.query) },
                onDeleteClick = { onDeleteSearchHistory(it.id) },
            )
        },
        key = { it.id },
    )
}

@Composable
private fun CategoryChipsRow(
    categories: List<Category>,
    selectedCategory: Category,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = contentPadding,
    ) {
        items(items = categories, contentType = { it }) {
            FilterChip(
                modifier = Modifier.animateItem(),
                selected = selectedCategory == it,
                onClick = { onCategoryClick(it) },
                label = { Text(text = it.name) },
            )
        }
    }
}