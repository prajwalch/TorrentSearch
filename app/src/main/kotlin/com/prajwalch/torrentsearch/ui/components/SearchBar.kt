package com.prajwalch.torrentsearch.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchBarState: SearchBarState = rememberSearchBarState(),
    textFieldState: TextFieldState = rememberTextFieldState(),
    content: @Composable (ColumnScope.() -> Unit),
) {
    val inputField = @Composable {
        SearchBarInputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearch,
        )
    }

    SearchBar(
        modifier = modifier,
        state = searchBarState,
        inputField = inputField,
    )
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarInputField(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    val defaultColors = SearchBarDefaults.inputFieldColors()
    val customColors = SearchBarDefaults.inputFieldColors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
    )
    val colors = when (searchBarState.currentValue) {
        SearchBarValue.Collapsed -> defaultColors
        else -> customColors
    }

    SearchBarDefaults.InputField(
        modifier = modifier,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = {
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            onSearch(it)
        },
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = {
            LeadingIcon(
                isSearchBarExpanded = searchBarState.currentValue == SearchBarValue.Expanded,
                onBack = {
                    coroutineScope.launch { searchBarState.animateToCollapsed() }
                }
            )
        },
        trailingIcon = {
            ClearIconButton(
                visible = textFieldState.text.isNotEmpty(),
                onClick = { textFieldState.clearText() },
            )
        },
        colors = colors,
    )
}

@Composable
private fun LeadingIcon(
    isSearchBarExpanded: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = isSearchBarExpanded,
    ) { searchBarExpanded ->
        when {
            searchBarExpanded -> NavigateBackIconButton(
                onClick = onBack,
                contentDescriptionId = R.string.desc_unfocus_search_bar,
            )

            else -> Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ClearIconButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        IconButton(modifier = modifier, onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.desc_clear_search_query),
            )
        }
    }
}