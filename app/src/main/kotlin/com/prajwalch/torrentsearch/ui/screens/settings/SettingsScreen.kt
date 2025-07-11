package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.Settings
import com.prajwalch.torrentsearch.ui.components.SettingsOptionMenu
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
        AppearanceSettings(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onOpenOptionMenu = { optionMenuEvent = it },
        )
        SearchSettings(
            settings = settings,
            onSettingsChange = onSettingsChange,
        )
    }
}

data class SettingsOptionMenuEvent(
    @param:StringRes
    val title: Int,
    val selectedOption: Int,
    val options: List<String>,
    val onSelected: (Int) -> Unit,
)