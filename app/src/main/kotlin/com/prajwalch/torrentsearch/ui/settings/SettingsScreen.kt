package com.prajwalch.torrentsearch.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.BuildConfig
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.DarkTheme
import com.prajwalch.torrentsearch.models.MaxNumResults
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.components.SettingsListItem
import com.prajwalch.torrentsearch.ui.components.SettingsSectionTitle
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.utils.categoryStringResource
import com.prajwalch.torrentsearch.utils.sortCriteriaStringResource
import com.prajwalch.torrentsearch.utils.sortOrderStringResource

private val LocalSettingsViewModel = compositionLocalOf<SettingsViewModel> {
    error("Local SettingsViewModel not provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDefaultCategory: () -> Unit,
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToDefaultSortOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = activityScopedViewModel<SettingsViewModel>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            SettingsScreenTopBar(
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding),
        ) {
            CompositionLocalProvider(LocalSettingsViewModel provides viewModel) {
                AppearanceSettings()
                GeneralSettings()
                SearchSettings(
                    onNavigateToSearchProviders = onNavigateToSearchProviders,
                    onNavigateToDefaultCategory = onNavigateToDefaultCategory,
                    onNavigateToDefaultSortOptions = onNavigateToDefaultSortOptions,
                )
                SearchHistorySettings()
                AdvancedSettings()
                About()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.settings_screen_title)) },
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun AppearanceSettings(modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.appearanceSettingsUiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SettingsSectionTitle(title = R.string.settings_section_appearance)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsListItem(
                onClick = { viewModel.enableDynamicTheme(!settings.enableDynamicTheme) },
                icon = R.drawable.ic_palette,
                headline = R.string.settings_enable_dynamic_theme,
                trailingContent = {
                    Switch(
                        checked = settings.enableDynamicTheme,
                        onCheckedChange = { viewModel.enableDynamicTheme(it) },
                    )
                },
            )
        }

        Box {
            var menuExpanded by remember(settings.darkTheme) { mutableStateOf(false) }

            SettingsListItem(
                onClick = { menuExpanded = true },
                icon = R.drawable.ic_dark_mode,
                headline = R.string.settings_dark_theme,
                supportingContent = darkThemeStringResource(settings.darkTheme),
            )

            RoundedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset = DpOffset(x = 16.dp, y = 0.dp),
            ) {
                DarkTheme.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(text = darkThemeStringResource(it)) },
                        onClick = { viewModel.setDarkTheme(it) },
                        leadingIcon = {
                            if (it == settings.darkTheme) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                )
                            }
                        }
                    )
                }
            }
        }

        SettingsListItem(
            onClick = { viewModel.enablePureBlackTheme(!settings.pureBlack) },
            icon = R.drawable.ic_contrast,
            headline = R.string.settings_pure_black,
            trailingContent = {
                Switch(
                    checked = settings.pureBlack,
                    onCheckedChange = { viewModel.enablePureBlackTheme(it) },
                )
            },
        )
    }
}

@Composable
private fun GeneralSettings(modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.generalSettingsUiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SettingsSectionTitle(title = R.string.settings_section_general)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val context = LocalContext.current

            SettingsListItem(
                onClick = { context.openAppLocaleSettings() },
                icon = R.drawable.ic_language,
                headline = R.string.settings_language,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                    )
                }
            )
        }

        SettingsListItem(
            onClick = { viewModel.enableNSFWMode(!settings.enableNSFWMode) },
            icon = R.drawable.ic_18_up_rating,
            headline = R.string.settings_enable_nsfw_mode,
            supportingContent = stringResource(R.string.settings_enable_nsfw_mode_summary),
            trailingContent = {
                Switch(
                    checked = settings.enableNSFWMode,
                    onCheckedChange = { viewModel.enableNSFWMode(it) },
                )
            },
        )
    }
}

@Composable
private fun SearchSettings(
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToDefaultCategory: () -> Unit,
    onNavigateToDefaultSortOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.searchSettingsUiState.collectAsStateWithLifecycle()

    var showMaxNumResultsDialog by remember { mutableStateOf(false) }

    if (showMaxNumResultsDialog) {
        MaxNumResultsDialog(
            onDismissRequest = { showMaxNumResultsDialog = false },
            num = if (settings.maxNumResults.isUnlimited()) null else settings.maxNumResults.n,
            onNumChange = { viewModel.setMaxNumResults(MaxNumResults(n = it)) },
            onUnlimitedClick = {
                showMaxNumResultsDialog = false
                viewModel.setMaxNumResults(MaxNumResults.Unlimited)
            },
        )
    }

    Column(modifier = modifier) {
        SettingsSectionTitle(title = R.string.settings_section_search)
        SettingsListItem(
            onClick = onNavigateToSearchProviders,
            icon = R.drawable.ic_travel_explore,
            headline = R.string.settings_search_providers,
            supportingContent = stringResource(
                R.string.settings_search_providers_summary_format,
                settings.searchProvidersStat.enabledSearchProvidersCount,
                settings.searchProvidersStat.totalSearchProvidersCount
            ),
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                )
            },
        )
        SettingsListItem(
            onClick = onNavigateToDefaultCategory,
            icon = R.drawable.ic_category_search,
            headline = R.string.settings_default_category,
            supportingContent = categoryStringResource(settings.defaultCategory),
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                )
            }
        )

        val defaultSortCriteria = sortCriteriaStringResource(
            settings.defaultSortOptions.sortCriteria,
        )
        val defaultSortOrder = sortOrderStringResource(
            settings.defaultSortOptions.sortOrder,
        )
        SettingsListItem(
            onClick = onNavigateToDefaultSortOptions,
            icon = R.drawable.ic_sort,
            headline = R.string.settings_default_sort_options,
            supportingContent = "$defaultSortCriteria / $defaultSortOrder",
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                )
            },
        )
        SettingsListItem(
            onClick = { showMaxNumResultsDialog = true },
            icon = R.drawable.ic_format_list_numbered,
            headline = R.string.settings_max_num_results,
            supportingContent = if (settings.maxNumResults.isUnlimited()) {
                stringResource(R.string.settings_max_num_results_button_unlimited)
            } else {
                settings.maxNumResults.n.toString()
            },
        )
    }
}

@Composable
private fun SearchHistorySettings(modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val settings by viewModel.searchHistorySettingsUiState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        SettingsSectionTitle(title = R.string.settings_section_search_history)
        SettingsListItem(
            onClick = { viewModel.enableSaveSearchHistory(!settings.saveSearchHistory) },
            icon = R.drawable.ic_search_activity,
            headline = R.string.settings_save_search_history,
            trailingContent = {
                Switch(
                    checked = settings.saveSearchHistory,
                    onCheckedChange = { viewModel.enableSaveSearchHistory(it) },
                )
            },
        )
        SettingsListItem(
            onClick = { viewModel.enableShowSearchHistory(!settings.showSearchHistory) },
            icon = R.drawable.ic_history_toggle_off,
            headline = R.string.settings_show_search_history,
            supportingContent = stringResource(R.string.settings_show_search_history_summary),
            trailingContent = {
                Switch(
                    checked = settings.showSearchHistory,
                    onCheckedChange = { viewModel.enableShowSearchHistory(it) },
                )
            },
        )
    }
}

@Composable
private fun AdvancedSettings(modifier: Modifier = Modifier) {
    val viewModel = LocalSettingsViewModel.current
    val uiState by viewModel.advanceSettingsUiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val packageManager = context.packageManager

    Column(modifier = modifier) {
        SettingsSectionTitle(R.string.settings_section_advanced)

        SettingsListItem(
            onClick = {
                viewModel.enableShareIntegration(
                    enable = !uiState.enableShareIntegration,
                    packageManager = packageManager,
                )
            },
            icon = R.drawable.ic_share,
            headline = R.string.settings_enable_share_integration,
            supportingContent = stringResource(
                R.string.settings_enable_share_integration_summary,
            ),
            trailingContent = {
                Switch(
                    checked = uiState.enableShareIntegration,
                    onCheckedChange = {
                        viewModel.enableShareIntegration(
                            enable = it,
                            packageManager = packageManager,
                        )
                    },
                )
            },
        )
        SettingsListItem(
            onClick = {
                viewModel.enableQuickSearch(
                    enable = !uiState.enableQuickSearch,
                    packageManager = packageManager,
                )
            },
            icon = R.drawable.ic_search,
            headline = R.string.settings_enable_quick_search,
            supportingContent = stringResource(
                R.string.settings_enable_quick_search_summary,
            ),
            trailingContent = {
                Switch(
                    checked = uiState.enableQuickSearch,
                    onCheckedChange = {
                        viewModel.enableQuickSearch(
                            enable = it,
                            packageManager = packageManager,
                        )
                    },
                )
            },
        )
    }
}

@Composable
private fun About(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    val repoUrl = "https://github.com/prajwalch/TorrentSearch"
    val currentReleaseUrl = "$repoUrl/releases/tag/v${BuildConfig.VERSION_NAME}"

    Column(modifier = modifier) {
        SettingsSectionTitle(title = R.string.settings_section_about)
        SettingsListItem(
            onClick = { uriHandler.openUri(uri = currentReleaseUrl) },
            icon = R.drawable.ic_info,
            headline = R.string.settings_version,
            supportingContent = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                )
            }
        )
        SettingsListItem(
            onClick = { uriHandler.openUri(uri = repoUrl) },
            icon = R.drawable.ic_code,
            headline = R.string.settings_source_code,
            supportingContent = repoUrl.removePrefix("https://"),
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                )
            }
        )
    }
}

@Composable
private fun MaxNumResultsDialog(
    onDismissRequest: () -> Unit,
    num: Int?,
    onNumChange: (Int) -> Unit,
    onUnlimitedClick: () -> Unit,
    modifier: Modifier = Modifier,
    sliderRange: ClosedFloatingPointRange<Float> = 10f..100f,
    incrementBy: Int = 5,
) {
    var sliderValue by remember(num) {
        mutableFloatStateOf(num?.toFloat() ?: sliderRange.start)
    }

    SettingsDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = R.string.settings_max_num_results,
        confirmButton = {
            TextButton(onClick = {
                onDismissRequest()
                onNumChange(sliderValue.toInt())
            }) {
                Text(text = stringResource(R.string.button_done))
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    R.string.settings_max_num_results_summary_format,
                    sliderValue.toInt()
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spaces.large))
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = (it / incrementBy) * incrementBy },
                valueRange = sliderRange,
                steps = ((sliderRange.endInclusive - sliderRange.start) / incrementBy).toInt() - 1,
            )
            OutlinedButton(onClick = onUnlimitedClick) {
                Text(text = stringResource(R.string.settings_max_num_results_button_unlimited))
            }
        }
    }
}

@Composable
private fun darkThemeStringResource(darkTheme: DarkTheme): String {
    val resId = when (darkTheme) {
        DarkTheme.On -> R.string.settings_dark_theme_on
        DarkTheme.Off -> R.string.settings_dark_theme_off
        DarkTheme.FollowSystem -> R.string.settings_dark_theme_follow_system
    }

    return stringResource(id = resId)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Context.openAppLocaleSettings() {
    val appUri = Uri.fromParts("package", this.packageName, null)
    val localeSettingsIntent = Intent().apply {
        action = Settings.ACTION_APP_LOCALE_SETTINGS
        data = appUri
    }

    this.startActivity(localeSettingsIntent)
}