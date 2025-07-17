package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsOptionMenu(
    @StringRes
    title: Int,
    options: List<String>,
    selectedOption: Int,
    onOptionSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDialog(
        modifier = modifier,
        title = title,
        onDismissRequest = onDismissRequest,
    ) {
        SettingsOptionMenuItems(
            items = options,
            selectedItem = selectedOption,
            onItemSelect = onOptionSelect,
        )
    }
}

@Composable
private fun SettingsOptionMenuItems(
    items: List<String>,
    selectedItem: Int,
    onItemSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(items) { index, item ->
            SettingsOptionMenuItem(
                selected = index == selectedItem,
                onSelect = { onItemSelect(index) },
                item = item,
            )
        }
    }
}

@Composable
private fun SettingsOptionMenuItem(
    selected: Boolean,
    onSelect: () -> Unit,
    item: String,
    modifier: Modifier = Modifier,
) {
    val listItemColors = ListItemDefaults.colors(containerColor = Color.Companion.Unspecified)

    ListItem(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onSelect),
        leadingContent = { RadioButton(selected = selected, onClick = onSelect) },
        headlineContent = { Text(text = item) },
        colors = listItemColors,
    )
}