package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun StackTraceCard(
    stackTrace: String,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    contentPadding: PaddingValues = PaddingValues(
        all = MaterialTheme.spaces.large,
    ),
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
    ) {
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(state = rememberScrollState()),
        ) {
            SelectionContainer { Text(text = stackTrace) }
        }
    }
}