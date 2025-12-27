package com.prajwalch.torrentsearch.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldLineLimits
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.R

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableSearchBar(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: SearchBarState = rememberSearchBarState(),
    textFieldState: TextFieldState = rememberTextFieldState(),
    placeholder: @Composable (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit),
) {
    val coroutineScope = rememberCoroutineScope()

    val inputFieldLeadingIcon: @Composable () -> Unit = @Composable {
        AnimatedContent(
            targetState = state.currentValue,
            contentAlignment = Alignment.Center,
            label = "Expandable search bar leading icon animation",
        ) { searchBarValue ->
            if (searchBarValue == SearchBarValue.Expanded) {
                ArrowBackIconButton(
                    onClick = { coroutineScope.launch { state.animateToCollapsed() } }
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                )
            }
        }
    }
    val inputField: @Composable () -> Unit = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = state,
            textFieldState = textFieldState,
            onSearch = onSearch,
            placeholder = placeholder,
            leadingIcon = inputFieldLeadingIcon,
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    ClearIconButton(onClick = { textFieldState.clearText() })
                }
            },
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

@Composable
fun CollapsibleSearchBar(
    state: CollapsibleSearchBarState,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
) {
    LaunchedEffect(state.isVisible) {
        if (!state.isVisible) return@LaunchedEffect

        state.focusSearchBar()
        state.observeText { onQueryChange(it) }
    }

    BackHandler(enabled = state.isVisible) {
        state.hideSearchBar()
    }

    if (state.isVisible) {
        TextField(
            modifier = modifier
                .focusRequester(state.focusRequester)
                .height(TextFieldDefaults.MinHeight),
            state = state.textFieldState,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = placeholder,
            trailingIcon = {
                if (!state.isTextEmpty) {
                    ClearIconButton(onClick = { state.clearText() })
                }
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

/** The state of a search bar which can be visible or hidden. */
@Stable
class CollapsibleSearchBarState(
    val textFieldState: TextFieldState,
    val focusRequester: FocusRequester,
    visibleOnInitial: Boolean = true,
) {
    var isVisible by mutableStateOf(visibleOnInitial)
        private set

    val isTextEmpty by derivedStateOf { textFieldState.text.isEmpty() }

    val isTextBlank by derivedStateOf { textFieldState.text.isBlank() }

    fun showSearchBar() {
        isVisible = true
    }

    fun hideSearchBar() {
        isVisible = false
    }

    fun clearText() {
        textFieldState.clearText()
    }

    suspend fun observeText(action: suspend (String) -> Unit) {
        snapshotFlow { textFieldState.text }
            // Ignore the initial empty text.
            .drop(1)
            .distinctUntilChanged()
            .collectLatest { action(it.toString()) }
    }

    fun focusSearchBar() {
        focusRequester.requestFocus()
    }

    companion object {
        fun Saver(
            textFieldState: TextFieldState,
            focusRequester: FocusRequester,
        ): Saver<CollapsibleSearchBarState, Boolean> {
            return Saver(
                save = { it.isVisible },
                restore = {
                    CollapsibleSearchBarState(
                        textFieldState = textFieldState,
                        focusRequester = focusRequester,
                        visibleOnInitial = it,
                    )
                }
            )
        }
    }
}

/** Create and remember a [CollapsibleSearchBarState]. */
@Composable
fun rememberCollapsibleSearchBarState(
    textFieldState: TextFieldState = rememberTextFieldState(""),
    focusRequester: FocusRequester = remember { FocusRequester() },
    visibleOnInitial: Boolean = true,
): CollapsibleSearchBarState {
    return rememberSaveable(
        saver = CollapsibleSearchBarState.Saver(
            textFieldState = textFieldState,
            focusRequester = focusRequester,
        )
    ) {
        CollapsibleSearchBarState(
            textFieldState = textFieldState,
            focusRequester = focusRequester,
            visibleOnInitial = visibleOnInitial,
        )
    }
}

// TODO: Rename it
@Composable
fun SearchBar(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
) {
    TextField(
        modifier = modifier.height(TextFieldDefaults.MinHeight),
        state = textFieldState,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = placeholder,
        trailingIcon = {
            if (textFieldState.text.isNotEmpty()) {
                ClearIconButton(onClick = { textFieldState.clearText() })
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun ClearIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
        )
    }
}