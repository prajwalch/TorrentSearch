package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.ui.theme.spaces

import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun LazyColumnWithScrollbar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    val scrollbarUnselectedColor = MaterialTheme.colorScheme.primary
    val scrollbarSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    LazyColumnScrollbar(
        state = state,
        settings = ScrollbarSettings.Default.copy(
            scrollbarPadding = MaterialTheme.spaces.extraSmall,
            thumbThickness = 8.dp,
            thumbMinLength = 0.07f,
            thumbUnselectedColor = scrollbarUnselectedColor,
            thumbSelectedColor = scrollbarSelectedColor,
            hideDelayMillis = 3000,
        ),
    ) {
        LazyColumn(
            modifier = modifier.imePadding(),
            state = state,
            contentPadding = contentPadding,
            content = content,
        )
    }
}