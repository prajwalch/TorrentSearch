package com.prajwalch.torrentsearch.ui.search

import android.os.Build

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
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

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        topBar = {
            SearchScreenTopBar(
                onNavigateToBookmarks = onNavigateToBookmarks,
                onNavigateToSettings = onNavigateToSettings,
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
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = MaterialTheme.spaces.extraLarge)
                .then(focusableModifier),
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

            SearchBar(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = {
                    onSearch(textFieldState.text.toString(), uiState.selectedCategory)
                },
            ) {
                SearchHistoryList(
                    histories = uiState.histories,
                    historyListItem = {
                        SearchHistoryListItem(
                            modifier = Modifier
                                .animateItem()
                                .clickable {
                                    onSearch(it.query, uiState.selectedCategory)
                                    textFieldState.setTextAndPlaceCursorAtEnd(it.query)
                                    coroutineScope.launch { searchBarState.animateToCollapsed() }
                                },
                            query = it.query,
                            onInsertClick = { textFieldState.setTextAndPlaceCursorAtEnd(it.query) },
                            onDeleteClick = { viewModel.deleteSearchHistory(it.id) },
                        )
                    },
                    key = { it.id },
                )
            }

            CategoryChipsRow(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::setCategory,
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spaces.large
                ),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spaces.small))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spaces.large),
                onClick = {
                    onSearch(textFieldState.text.toString(), uiState.selectedCategory)
                },
                enabled = textFieldState.text.isNotBlank(),
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
            SettingsIconButton(onClick = onNavigateToSettings)
        }
    )
}