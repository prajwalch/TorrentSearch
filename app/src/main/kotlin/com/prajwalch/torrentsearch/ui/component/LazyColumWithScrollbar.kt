package com.prajwalch.torrentsearch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.ui.theme.spaces

import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun LazyColumnWithScrollbar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    LazyColumnScrollbar(
        state = state,
        settings = ScrollbarSettings.Default.copy(
            scrollbarPadding = MaterialTheme.spaces.extraSmall,
            thumbThickness = 4.dp,
            thumbMinLength = 0.07f,
            thumbUnselectedColor = Color.Gray,
            thumbSelectedColor = Color.DarkGray,
            hideDelayMillis = 3000,
        ),
    ) {
        LazyColumn(
            modifier = modifier.imePadding(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}