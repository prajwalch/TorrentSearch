package com.prajwalch.torrentsearch.ui.home.component

import android.os.Build

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.component.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.search.component.ExpandableSearchBar
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBox(
    onSearch: (String) -> Unit,
    onBrowse: () -> Unit,
    selectedCategory: Category,
    categories: List<Category>,
    onCategorySelect: (Category) -> Unit,
    suggestions: List<String>,
    onFilterSuggestions: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val enableSearchButton by remember {
        derivedStateOf {
            textFieldState.text.isNotBlank()
        }
    }

    val categoryChipsRow: @Composable () -> Unit = @Composable {
        CategoryChipsRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategoryClick = onCategorySelect,
            contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
        )
    }
    // Prevent search bar from being autofocused on older Android versions
    // which range from 7.1 to 8.1 (<9).
    val focusableModifier = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        Modifier.focusable()
    } else {
        Modifier
    }

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }
            // Ignore the initial empty text.
            .drop(1)
            .distinctUntilChanged()
            .collectLatest { onFilterSuggestions(it.toString()) }
    }

    Column(
        modifier = modifier.then(focusableModifier),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExpandableSearchBar(
            state = searchBarState,
            textFieldState = textFieldState,
            onSearch = {
                if (textFieldState.text.isNotBlank()) {
                    onSearch(textFieldState.text.toString())
                    coroutineScope.launch { searchBarState.animateToCollapsed() }
                }
            },
        ) {
            categoryChipsRow()
            SearchSuggestionList(
                queries = suggestions,
                onSearchRequest = {
                    onSearch(it)
                    textFieldState.setTextAndPlaceCursorAtEnd(it)
                    coroutineScope.launch { searchBarState.animateToCollapsed() }
                },
                onInsertQuery = textFieldState::setTextAndPlaceCursorAtEnd,
            )
        }

        categoryChipsRow()

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchButton(
                onClick = { onSearch(textFieldState.text.toString()) },
                enabled = enableSearchButton,
            )
            Spacer(Modifier.width(MaterialTheme.spaces.small))
            BrowseButton(onClick = onBrowse)
        }
    }
}

@Preview
@Composable
private fun SearchBoxPreview() {
    TorrentSearchTheme {
        SearchBox(
            onSearch = {},
            onBrowse = {},
            selectedCategory = Category.All,
            categories = Category.entries,
            onCategorySelect = {},
            suggestions = emptyList(),
            onFilterSuggestions = {},
        )
    }
}