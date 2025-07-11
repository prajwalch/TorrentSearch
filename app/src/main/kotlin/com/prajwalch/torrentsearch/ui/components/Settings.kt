package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.R

@Composable
fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(16.dp),
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
fun SettingsListItem(
    @DrawableRes
    leadingIconId: Int,
    headline: String,
    modifier: Modifier = Modifier,
    supportingContent: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                painter = painterResource(leadingIconId),
                contentDescription = headline,
            )
        },
        headlineContent = { Text(text = headline) },
        supportingContent = supportingContent?.let { { Text(text = it) } },
        trailingContent = trailingContent,
    )
}

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

@Composable
fun SettingsDialog(
    @StringRes
    title: Int,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.settings_dialog_button_cancel))
            }
        },
        confirmButton = {},
        title = { Text(text = stringResource(title)) },
        text = content,
    )
}