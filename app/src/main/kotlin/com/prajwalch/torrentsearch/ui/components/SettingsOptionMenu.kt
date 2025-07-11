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
    selectedOption: Int,
    options: List<String>,
    onSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = title,
        content = {
            SettingsOptionMenuItems(
                selectedItem = selectedOption,
                items = options,
                onSelect = onSelected,
            )
        }
    )
}

@Composable
private fun SettingsOptionMenuItems(
    selectedItem: Int,
    items: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(items) { index, item ->
            SettingsOptionMenuItem(
                selected = index == selectedItem,
                item = item,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun SettingsOptionMenuItem(
    selected: Boolean,
    item: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listItemColors = ListItemDefaults.colors(containerColor = Color.Companion.Unspecified)

    ListItem(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        headlineContent = { Text(text = item) },
        colors = listItemColors,
    )
}