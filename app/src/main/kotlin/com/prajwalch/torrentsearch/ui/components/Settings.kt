package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prajwalch.torrentsearch.R

@Composable
fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.titleSmall,
) {
    Text(
        modifier = modifier.padding(16.dp),
        text = title,
        color = color,
        style = style,
    )
}

@Composable
fun SettingsItem(
    @DrawableRes
    leadingIconId: Int,
    headline: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
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
fun SettingsDialog(
    @StringRes
    title: Int,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .then(modifier),
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.settings_dialog_button_cancel))
            }
        },
        confirmButton = confirmButton ?: {},
        title = { Text(text = stringResource(title)) },
        text = content,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    )
}

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