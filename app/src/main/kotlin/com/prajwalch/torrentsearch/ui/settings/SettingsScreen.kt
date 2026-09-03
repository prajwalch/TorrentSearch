package com.prajwalch.torrentsearch.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.BuildConfig
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.DarkTheme
import com.prajwalch.torrentsearch.domain.model.DohProvider
import com.prajwalch.torrentsearch.domain.model.MaxNumResults
import com.prajwalch.torrentsearch.ui.darkThemeStringResource
import com.prajwalch.torrentsearch.ui.settings.component.ClearViewedTorrentsDialog
import com.prajwalch.torrentsearch.ui.settings.component.DarkThemeOption
import com.prajwalch.torrentsearch.ui.settings.component.DohProvidersMenu
import com.prajwalch.torrentsearch.ui.settings.component.MaxNumResultsDialog
import com.prajwalch.torrentsearch.ui.settings.component.SectionTitle
import com.prajwalch.torrentsearch.ui.settings.component.SettingsGroup
import com.prajwalch.torrentsearch.ui.settings.component.SettingsItemCard
import com.prajwalch.torrentsearch.ui.settings.component.SettingsListItem
import com.prajwalch.torrentsearch.ui.sortCriteriaStringResource
import com.prajwalch.torrentsearch.ui.sortOrderStringResource
import com.prajwalch.torrentsearch.ui.theme.spaces

import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToDefaultSortOptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val contentResolver = LocalContext.current.contentResolver
    val logsExportLocationChooser = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(TorrentSearchConstants.LOGS_FILE_TYPE),
    ) { fileUri ->
        fileUri
            ?.let(contentResolver::openOutputStream)
            ?.let(viewModel::exportLogs)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsScreenTopBar(
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding + PaddingValues(horizontal = MaterialTheme.spaces.large))
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        ) {
            AppearanceSection(
                uiState = uiState.appearanceSettings,
                onEnableDynamicTheme = viewModel::enableDynamicTheme,
                onSetDarkTheme = viewModel::setDarkTheme,
                onEnablePureBlackTheme = viewModel::enablePureBlackTheme,
            )

            val packageManager = LocalContext.current.packageManager
            GeneralSection(
                uiState = uiState.generalSettings,
                onEnableOpenTorrentDetailsInApp = viewModel::enableOpenTorrentDetailsInApp,
                onEnableShareIntegration = { viewModel.enableShareIntegration(it, packageManager) },
                onEnableQuickSearch = { viewModel.enableQuickSearch(it, packageManager) },
            )

            ContentAndPrivacySection(
                uiState = uiState.contentAndPrivacySettings,
                onEnableNSFWMode = viewModel::enableNSFWMode,
                onEnableBlurNSFWImages = viewModel::enableBlurNSFWImages,
                onClearViewedTorrents = viewModel::clearViewedTorrents,
                onEnableSaveSearchHistory = viewModel::enableSaveSearchHistory,
                onEnableShowSearchHistory = viewModel::enableShowSearchHistory,
            )

            SearchSection(
                uiState = uiState.searchSettings,
                onNavigateToSearchProviders = onNavigateToSearchProviders,
                onNavigateToDefaultSortOptions = onNavigateToDefaultSortOptions,
                onSetMaxNumResults = viewModel::setMaxNumResults,
            )

            NetworkSection(
                uiState = uiState.networkSettings,
                onSetDohProvider = viewModel::setDohProvider,
            )

            AboutSection(
                onExportLogsToFile = {
                    logsExportLocationChooser.launch(TorrentSearchConstants.APP_LOGS_FILE_NAME)
                },
            )

            Spacer(Modifier.height(MaterialTheme.spaces.large))
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
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun AppearanceSection(
    uiState: AppearanceSettingsUiState,
    onEnableDynamicTheme: (Boolean) -> Unit,
    onSetDarkTheme: (DarkTheme) -> Unit,
    onEnablePureBlackTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        SectionTitle(stringResource(R.string.settings_group_appearance))

        SettingsGroup {
            // Dynamic theme is available only on Android 12+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsListItem(
                    onClick = { onEnableDynamicTheme(!uiState.enableDynamicTheme) },
                    leadingIcon = painterResource(R.drawable.ic_palette),
                    title = stringResource(R.string.settings_enable_dynamic_theme),
                    subtitle = stringResource(R.string.settings_enable_dynamic_theme_summary),
                    trailingContent = {
                        Switch(
                            checked = uiState.enableDynamicTheme,
                            onCheckedChange = onEnableDynamicTheme,
                        )
                    },
                )
            }

            SettingsItemCard(
                leadingIcon = painterResource(R.drawable.ic_dark_mode),
                title = stringResource(R.string.settings_dark_theme),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small)) {
                    DarkTheme.entries.forEach {
                        DarkThemeOption(
                            modifier = Modifier.weight(1f),
                            onClick = { onSetDarkTheme(it) },
                            selected = uiState.darkTheme == it,
                            icon = painterResource(it.iconResId()),
                            label = darkThemeStringResource(it),
                        )
                    }
                }
            }

            SettingsListItem(
                onClick = { onEnablePureBlackTheme(!uiState.pureBlack) },
                leadingIcon = painterResource(R.drawable.ic_contrast),
                title = stringResource(R.string.settings_pure_black),
                subtitle = stringResource(R.string.settings_pure_black_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.pureBlack,
                        onCheckedChange = onEnablePureBlackTheme,
                    )
                },
            )
        }
    }
}

@DrawableRes
private fun DarkTheme.iconResId(): Int = when (this) {
    DarkTheme.On -> R.drawable.ic_dark_mode
    DarkTheme.Off -> R.drawable.ic_light_mode
    DarkTheme.FollowSystem -> R.drawable.ic_phone_android
}

@Composable
private fun GeneralSection(
    uiState: GeneralSettingsUiState,
    onEnableOpenTorrentDetailsInApp: (Boolean) -> Unit,
    onEnableShareIntegration: (Boolean) -> Unit,
    onEnableQuickSearch: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        SectionTitle(stringResource(R.string.settings_group_general))

        SettingsGroup {
            // Per-app language preferences is available only on Android 13+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val context = LocalContext.current

                SettingsListItem(
                    onClick = { context.openAppLocaleSettings() },
                    leadingIcon = painterResource(R.drawable.ic_language),
                    title = stringResource(R.string.settings_language),
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = null,
                        )
                    },
                )
            }

            SettingsListItem(
                onClick = { onEnableOpenTorrentDetailsInApp(!uiState.openTorrentDetailsInApp) },
                leadingIcon = painterResource(R.drawable.ic_link),
                title = stringResource(R.string.settings_open_torrent_details_in_app),
                subtitle = stringResource(R.string.settings_open_torrent_details_in_app_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.openTorrentDetailsInApp,
                        onCheckedChange = onEnableOpenTorrentDetailsInApp,
                    )
                },
            )

            SettingsListItem(
                onClick = { onEnableShareIntegration(!uiState.enableShareIntegration) },
                leadingIcon = painterResource(R.drawable.ic_share),
                title = stringResource(R.string.settings_enable_share_integration),
                subtitle = stringResource(R.string.settings_enable_share_integration_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.enableShareIntegration,
                        onCheckedChange = onEnableShareIntegration,
                    )
                },
            )

            SettingsListItem(
                onClick = { onEnableQuickSearch(!uiState.enableQuickSearch) },
                leadingIcon = painterResource(R.drawable.ic_search),
                title = stringResource(R.string.settings_enable_quick_search),
                subtitle = stringResource(R.string.settings_enable_quick_search_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.enableQuickSearch,
                        onCheckedChange = onEnableQuickSearch,
                    )
                },
            )
        }
    }
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

@Composable
private fun ContentAndPrivacySection(
    uiState: ContentAndPrivacySettingsUiState,
    onEnableNSFWMode: (Boolean) -> Unit,
    onEnableBlurNSFWImages: (Boolean) -> Unit,
    onClearViewedTorrents: () -> Unit,
    onEnableSaveSearchHistory: (Boolean) -> Unit,
    onEnableShowSearchHistory: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearViewedTorrentsDialog by rememberSaveable { mutableStateOf(false) }
    if (showClearViewedTorrentsDialog) {
        ClearViewedTorrentsDialog(
            onDismiss = { showClearViewedTorrentsDialog = false },
            onConfirm = {
                onClearViewedTorrents()
                showClearViewedTorrentsDialog = false
            },
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        SectionTitle(stringResource(R.string.settings_group_content_and_privacy))

        SettingsGroup {
            SettingsListItem(
                onClick = { onEnableNSFWMode(!uiState.enableNSFWMode) },
                leadingIcon = painterResource(R.drawable.ic_18_up_rating),
                title = stringResource(R.string.settings_enable_nsfw_mode),
                subtitle = stringResource(R.string.settings_enable_nsfw_mode_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.enableNSFWMode,
                        onCheckedChange = onEnableNSFWMode,
                    )
                },
            )

            SettingsListItem(
                onClick = { onEnableBlurNSFWImages(!uiState.blurNSFWImages) },
                leadingIcon = painterResource(R.drawable.ic_18_up_rating),
                title = stringResource(R.string.settings_blur_nsfw_images),
                subtitle = stringResource(R.string.settings_blur_nsfw_images_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.blurNSFWImages,
                        onCheckedChange = onEnableBlurNSFWImages,
                    )
                },
            )

            SettingsListItem(
                onClick = { showClearViewedTorrentsDialog = true },
                leadingIcon = painterResource(R.drawable.ic_history),
                title = stringResource(R.string.settings_clear_viewed_torrents),
                subtitle = stringResource(R.string.settings_clear_viewed_torrents_summary),
            )

            SettingsListItem(
                onClick = { onEnableSaveSearchHistory(!uiState.saveSearchHistory) },
                leadingIcon = painterResource(R.drawable.ic_search_activity),
                title = stringResource(R.string.settings_save_search_history),
                subtitle = stringResource(R.string.settings_save_search_history_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.saveSearchHistory,
                        onCheckedChange = onEnableSaveSearchHistory,
                    )
                },
            )

            SettingsListItem(
                onClick = { onEnableShowSearchHistory(!uiState.showSearchHistory) },
                leadingIcon = painterResource(R.drawable.ic_history_toggle_off),
                title = stringResource(R.string.settings_show_search_history),
                subtitle = stringResource(R.string.settings_show_search_history_summary),
                trailingContent = {
                    Switch(
                        checked = uiState.showSearchHistory,
                        onCheckedChange = onEnableShowSearchHistory,
                    )
                },
            )
        }
    }
}

@Composable
private fun SearchSection(
    uiState: SearchSettingsUiState,
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToDefaultSortOptions: () -> Unit,
    onSetMaxNumResults: (MaxNumResults) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMaxNumResultsDialog by rememberSaveable { mutableStateOf(false) }
    if (showMaxNumResultsDialog) {
        MaxNumResultsDialog(
            onDismiss = { showMaxNumResultsDialog = false },
            num = if (uiState.maxNumResults.isUnlimited()) null else uiState.maxNumResults.n,
            onNumChange = { onSetMaxNumResults(MaxNumResults(n = it)) },
            onUnlimitedClick = {
                onSetMaxNumResults(MaxNumResults.Unlimited)
                showMaxNumResultsDialog = false
            },
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        SectionTitle(stringResource(R.string.settings_group_search))

        SettingsGroup {
            SettingsListItem(
                onClick = onNavigateToSearchProviders,
                leadingIcon = painterResource(R.drawable.ic_hub),
                title = stringResource(R.string.settings_search_providers),
                subtitle = stringResource(
                    R.string.settings_search_providers_summary_format,
                    uiState.searchProvidersStat.enabledSearchProvidersCount,
                    uiState.searchProvidersStat.totalSearchProvidersCount,
                ),
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                    )
                },
            )

            val defaultSortOptions = uiState.defaultSortOptions
            val defaultSortCriteria = sortCriteriaStringResource(defaultSortOptions.criteria)
            val defaultSortOrder = sortOrderStringResource(defaultSortOptions.order)

            SettingsListItem(
                onClick = onNavigateToDefaultSortOptions,
                leadingIcon = painterResource(R.drawable.ic_sort),
                title = stringResource(R.string.settings_default_sort_options),
                subtitle = "$defaultSortCriteria / $defaultSortOrder",
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                    )
                },
            )

            SettingsListItem(
                onClick = { showMaxNumResultsDialog = true },
                leadingIcon = painterResource(R.drawable.ic_format_list_numbered),
                title = stringResource(R.string.settings_max_num_results),
                subtitle = if (uiState.maxNumResults.isUnlimited()) {
                    stringResource(R.string.settings_max_num_results_button_unlimited)
                } else {
                    uiState.maxNumResults.n.toString()
                },
            )
        }
    }
}

@Composable
private fun NetworkSection(
    uiState: NetworkSettingsUiState,
    onSetDohProvider: (DohProvider) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        SectionTitle(stringResource(R.string.settings_group_network))

        SettingsGroup {
            SettingsItemCard(
                leadingIcon = painterResource(R.drawable.ic_dns),
                title = stringResource(R.string.settings_dns_over_https),
                subtitle = stringResource(R.string.settings_dns_over_https_summary),
            ) {
                var showDohProviders by rememberSaveable(uiState.dohProvider) {
                    mutableStateOf(false)
                }
                DohProvidersMenu(
                    modifier = Modifier.padding(start = 40.dp),
                    expanded = showDohProviders,
                    onExpandedChange = { showDohProviders = it },
                    selectedDohProvider = uiState.dohProvider,
                    onDohProviderSelect = onSetDohProvider,
                )
            }
        }
    }
}

@Composable
private fun AboutSection(
    onExportLogsToFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large)
    ) {
        SectionTitle(stringResource(R.string.settings_group_about))

        SettingsGroup {
            SettingsListItem(
                onClick = onExportLogsToFile,
                leadingIcon = painterResource(R.drawable.ic_file_export),
                title = stringResource(R.string.settings_export_logs_to_file),
                subtitle = stringResource(R.string.settings_export_logs_to_file_summary),
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                    )
                },
            )

            SettingsListItem(
                onClick = { uriHandler.openUri(TorrentSearchConstants.GITHUB_RELEASE_URL) },
                leadingIcon = painterResource(R.drawable.ic_info),
                title = stringResource(R.string.settings_version),
                subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_outward),
                        contentDescription = null,
                    )
                },
            )

            SettingsListItem(
                onClick = { uriHandler.openUri(TorrentSearchConstants.GITHUB_REPO_URL) },
                leadingIcon = painterResource(R.drawable.ic_code),
                title = stringResource(R.string.settings_source_code),
                subtitle = TorrentSearchConstants.GITHUB_REPO_URL,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_outward),
                        contentDescription = null,
                    )
                },
            )
        }
    }
}