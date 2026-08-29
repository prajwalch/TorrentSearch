package com.prajwalch.torrentsearch.ui.search.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableSearchBar(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: SearchBarState = rememberSearchBarState(),
    textFieldState: TextFieldState = rememberTextFieldState(),
    content: @Composable (ColumnScope.() -> Unit),
) {
    val inputField: @Composable () -> Unit = @Composable {
        SearchBarInputField(
            textFieldState = textFieldState,
            searchBarState = state,
            onSearch = onSearch,
        )
    }

    SearchBar(
        modifier = modifier,
        state = state,
        inputField = inputField,
    )
    ExpandedFullScreenSearchBar(
        state = state,
        inputField = inputField,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val showClearTextButton by remember {
        derivedStateOf { textFieldState.text.isNotEmpty() }
    }

    SearchBarDefaults.InputField(
        modifier = modifier,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = onSearch,
        placeholder = { Text(stringResource(R.string.home_search_query_hint)) },
        leadingIcon = {
            SearchBarLeadingContent(
                searchBarValue = searchBarState.currentValue,
                onCollapseSearchBar = {
                    coroutineScope.launch { searchBarState.animateToCollapsed() }
                },
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = showClearTextButton,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(onClick = { textFieldState.clearText() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarLeadingContent(
    searchBarValue: SearchBarValue,
    onCollapseSearchBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = searchBarValue,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentAlignment = Alignment.Center,
    ) { targetSearchBarValue ->
        if (targetSearchBarValue == SearchBarValue.Expanded) {
            IconButton(onClick = onCollapseSearchBar) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
            )
        }
    }
}