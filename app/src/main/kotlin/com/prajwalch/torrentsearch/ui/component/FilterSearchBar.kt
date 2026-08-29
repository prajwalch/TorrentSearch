package com.prajwalch.torrentsearch.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme

@Composable
fun FilterSearchBar(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
) {
    val showClearTextButton by remember {
        derivedStateOf { textFieldState.text.isNotEmpty() }
    }
    val focusRequester = remember { FocusRequester() }

    // Focus search bar as soon as it becomes visible.
    SideEffect(Unit) {
        focusRequester.requestFocus()
    }

    TextField(
        modifier = modifier
            .requiredHeight(TextFieldDefaults.MinHeight)
            .focusRequester(focusRequester),
        state = textFieldState,
        placeholder = placeholder,
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
        textStyle = MaterialTheme.typography.bodyLarge,
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Preview
@Composable
private fun FilterSearchBarPreview() {
    TorrentSearchTheme {
        FilterSearchBar(rememberTextFieldState())
    }
}