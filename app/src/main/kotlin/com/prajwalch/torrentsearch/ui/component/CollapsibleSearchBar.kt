package com.prajwalch.torrentsearch.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

import com.prajwalch.torrentsearch.R

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

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
                AnimatedVisibility(!state.isTextEmpty) {
                    IconButton(onClick = { state.clearText() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                        )
                    }
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