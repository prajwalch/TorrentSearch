package com.prajwalch.torrentsearch.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.DarkTheme
import com.prajwalch.torrentsearch.data.Settings
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SettingsScreenTopBar(onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        SettingsScreenContent(
            modifier = Modifier.padding(innerPadding),
            settings = settings,
            onSettingsChange = viewModel::updateSettings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenTopBar(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.settings_screen_title)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.button_go_back_to_search_screen)
                )
            }
        }
    )
}

private data class SettingsOptionMenuEvent(
    @param:StringRes
    val title: Int,
    val selectedOption: Int,
    val options: List<String>,
    val onSelected: (Int) -> Unit,
)

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
) {
    var optionMenuEvent by remember(settings) { mutableStateOf<SettingsOptionMenuEvent?>(null) }

    optionMenuEvent?.let { event ->
        SettingsOptionMenu(
            title = event.title,
            selectedOption = event.selectedOption,
            options = event.options,
            onSelected = event.onSelected,
            onDismissRequest = { optionMenuEvent = null },
        )
    }

    Column(modifier = modifier) {
        SettingsSectionTitle(title = stringResource(R.string.settings_section_appearance))
        SettingsListItem(
            leadingIconId = R.drawable.ic_palette,
            headline = stringResource(R.string.setting_enable_dynamic_theme),
            trailingContent = {
                Switch(checked = settings.enableDynamicTheme, onCheckedChange = {
                    onSettingsChange(settings.copy(enableDynamicTheme = it))
                })
            },
            onClick = {
                onSettingsChange(
                    settings.copy(enableDynamicTheme = !settings.enableDynamicTheme)
                )
            }
        )
        SettingsListItem(
            leadingIconId = R.drawable.ic_dark_mode,
            headline = stringResource(R.string.setting_dark_theme),
            supportingContent = settings.darkTheme.toString(),
            onClick = {
                optionMenuEvent = SettingsOptionMenuEvent(
                    title = R.string.setting_dark_theme,
                    selectedOption = settings.darkTheme.ordinal,
                    options = DarkTheme.entries.map { it.toString() },
                    onSelected = {
                        onSettingsChange(
                            settings.copy(darkTheme = DarkTheme.fromInt(it))
                        )
                    }
                )
            }
        )

        SettingsSectionTitle(stringResource(R.string.settings_section_search))
        SettingsListItem(
            leadingIconId = R.drawable.ic_18_up_rating,
            headline = stringResource(R.string.setting_enable_nsfw_search),
            trailingContent = {
                Switch(
                    checked = settings.enableNSFWSearch,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(enableNSFWSearch = it))
                    }
                )
            },
            onClick = {
                onSettingsChange(
                    settings.copy(enableNSFWSearch = !settings.enableNSFWSearch)
                )
            }
        )
        SettingsListItem(
            leadingIconId = R.drawable.ic_graph,
            headline = stringResource(R.string.setting_search_providers),
            onClick = {}
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(16.dp),
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SettingsListItem(
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
private fun SettingsOptionMenu(
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
    val listItemColors = ListItemDefaults.colors(containerColor = Color.Unspecified)

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
private fun SettingsDialog(
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