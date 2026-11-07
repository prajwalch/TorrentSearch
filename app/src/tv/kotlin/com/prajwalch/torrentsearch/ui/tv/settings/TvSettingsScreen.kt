package com.prajwalch.torrentsearch.ui.tv.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.DarkTheme
import com.prajwalch.torrentsearch.domain.model.DohProvider
import com.prajwalch.torrentsearch.domain.model.MaxNumResults
import com.prajwalch.torrentsearch.ui.settings.SettingsViewModel
import com.prajwalch.torrentsearch.ui.tv.component.TvActionButton
import com.prajwalch.torrentsearch.ui.tv.component.TvChip
import com.prajwalch.torrentsearch.ui.tv.component.TvChipRow
import com.prajwalch.torrentsearch.ui.tv.component.TvFocusDefaults
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvMenuItem
import com.prajwalch.torrentsearch.ui.tv.component.TvOptionDialog
import com.prajwalch.torrentsearch.ui.tv.component.TvSectionHeader
import com.prajwalch.torrentsearch.ui.tv.component.TvSettingRow
import com.prajwalch.torrentsearch.ui.tv.component.TvTopBar
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding
import com.prajwalch.torrentsearch.ui.tv.theme.spaces
import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface

import org.koin.androidx.compose.koinViewModel

/**
 * TV settings.
 *
 * Differences from the handheld `SettingsScreen`:
 *
 * - Every toggle is a full-width row that flips on OK. A `Switch` is unusable with
 *   a D-pad: the thumb is a few pixels and there is no way to hit it reliably.
 * - Multi-value settings (dark theme, max results, DoH provider) open a radio
 *   dialog instead of a `DropdownMenu`, which has no usable D-pad semantics.
 * - The share / quick-search / per-app-language rows are hidden. They toggle
 *   handheld-only activity aliases and `ACTION_APP_LOCALE_SETTINGS`, neither of
 *   which exists on a TV. `SettingsViewModel.enableShareIntegration` also needs a
 *   `PackageManager`, which the TV variant has no use for.
 */
@Composable
fun TvSettingsScreen(
    onNavigateToSearchProviders: () -> Unit,
    onNavigateToDefaultSortOptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showClearViewedConfirm by remember { mutableStateOf(false) }
    var darkThemeDialog by remember { mutableStateOf(false) }
    var maxResultsDialog by remember { mutableStateOf(false) }
    var dohDialog by remember { mutableStateOf(false) }

    val exportLogsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TorrentSearchConstants.LOGS_FILE_TYPE),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let(viewModel::exportLogs)
        }
    }

    // Screen entry point: focus lands on the first setting row when this route opens.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(title = stringResource(R.string.settings_screen_title))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = tvListContentPadding(),
            verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
        ) {
            item { TvSectionHeader(stringResource(R.string.settings_group_appearance)) }

            item {
                TvSettingRow(
                    modifier = entryFocusRequester
                        ?.let { Modifier.focusRequester(it) } ?: Modifier,
                    title = stringResource(R.string.settings_enable_dynamic_theme),
                    value = onOff(uiState.appearanceSettings.enableDynamicTheme),
                    onClick = {
                        viewModel.enableDynamicTheme(
                            !uiState.appearanceSettings.enableDynamicTheme,
                        )
                    },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_dark_theme),
                    value = stringResource(uiState.appearanceSettings.darkTheme.labelRes()),
                    onClick = { darkThemeDialog = true },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_pure_black),
                    value = onOff(uiState.appearanceSettings.pureBlack),
                    onClick = {
                        viewModel.enablePureBlackTheme(!uiState.appearanceSettings.pureBlack)
                    },
                )
            }

            item { TvSectionHeader(stringResource(R.string.settings_group_general)) }

            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_open_torrent_details_in_app),
                    value = onOff(uiState.generalSettings.openTorrentDetailsInApp),
                    onClick = {
                        viewModel.enableOpenTorrentDetailsInApp(
                            !uiState.generalSettings.openTorrentDetailsInApp,
                        )
                    },
                )
            }

            item {
                TvSectionHeader(stringResource(R.string.settings_group_content_and_privacy))
            }

            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_enable_nsfw_mode),
                    value = onOff(uiState.contentAndPrivacySettings.enableNSFWMode),
                    onClick = {
                        viewModel.enableNSFWMode(
                            !uiState.contentAndPrivacySettings.enableNSFWMode,
                        )
                    },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_blur_nsfw_images),
                    value = onOff(uiState.contentAndPrivacySettings.blurNSFWImages),
                    onClick = {
                        viewModel.enableBlurNSFWImages(
                            !uiState.contentAndPrivacySettings.blurNSFWImages,
                        )
                    },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_clear_viewed_torrents),
                    value = stringResource(
                        R.string.settings_max_num_results_summary_format,
                        uiState.contentAndPrivacySettings.viewedTorrentsCount,
                    ),
                    onClick = { showClearViewedConfirm = true },
                    enabled = uiState.contentAndPrivacySettings.viewedTorrentsCount > 0,
                )
            }

            item { TvSectionHeader(stringResource(R.string.settings_group_search)) }

            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_search_providers),
                    value = stringResource(
                        R.string.settings_search_providers_summary_format,
                        uiState.searchSettings.searchProvidersStat.enabledSearchProvidersCount,
                        uiState.searchSettings.searchProvidersStat.totalSearchProvidersCount,
                    ),
                    onClick = onNavigateToSearchProviders,
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_default_sort_options),
                    value = "${uiState.searchSettings.defaultSortOptions.criteria.name} · " +
                        uiState.searchSettings.defaultSortOptions.order.name,
                    onClick = onNavigateToDefaultSortOptions,
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_max_num_results),
                    value = uiState.searchSettings.maxNumResults.displayText(),
                    onClick = { maxResultsDialog = true },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_save_search_history),
                    value = onOff(uiState.contentAndPrivacySettings.saveSearchHistory),
                    onClick = {
                        viewModel.enableSaveSearchHistory(
                            !uiState.contentAndPrivacySettings.saveSearchHistory,
                        )
                    },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_show_search_history),
                    value = onOff(uiState.contentAndPrivacySettings.showSearchHistory),
                    onClick = {
                        viewModel.enableShowSearchHistory(
                            !uiState.contentAndPrivacySettings.showSearchHistory,
                        )
                    },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_show_recent_searches),
                    value = onOff(uiState.contentAndPrivacySettings.showRecentSearches),
                    onClick = {
                        viewModel.enableShowRecentSearches(
                            !uiState.contentAndPrivacySettings.showRecentSearches,
                        )
                    },
                )
            }

            item { TvSectionHeader(stringResource(R.string.settings_group_network)) }

            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_dns_over_https),
                    value = uiState.networkSettings.dohProvider.id,
                    onClick = { dohDialog = true },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_check_providers),
                    value = onOff(uiState.networkSettings.checkProvidersOnStartup),
                    onClick = {
                        viewModel.enableCheckProvidersOnStartup(
                            !uiState.networkSettings.checkProvidersOnStartup,
                        )
                    },
                )
            }
            item {
                TvSettingRow(
                    title = stringResource(R.string.settings_export_logs_to_file),
                    value = "",
                    onClick = { exportLogsLauncher.launch(TorrentSearchConstants.APP_LOGS_FILE_NAME) },
                )
            }
        }
    }

    if (darkThemeDialog) {
        TvOptionDialog(
            title = stringResource(R.string.settings_dark_theme),
            options = DarkTheme.entries.map {
                Triple(
                    stringResource(it.labelRes()),
                    it == uiState.appearanceSettings.darkTheme,
                ) { viewModel.setDarkTheme(it); darkThemeDialog = false }
            },
            onDismiss = { darkThemeDialog = false },
        )
    }

    if (maxResultsDialog) {
        TvOptionDialog(
            title = stringResource(R.string.settings_max_num_results),
            // MaxNumResults is a value class, not an enum, so the ladder is explicit.
            // The handheld screen uses a Slider, which is close to unusable with a
            // D-pad (and can overshoot wildly on a single LEFT/RIGHT press).
            options = (listOf(MaxNumResults.Unlimited) +
                (10..100 step 10).map { MaxNumResults(it) }).map { option ->
                Triple(
                    option.displayText(),
                    option == uiState.searchSettings.maxNumResults,
                ) {
                    viewModel.setMaxNumResults(option)
                    maxResultsDialog = false
                }
            },
            onDismiss = { maxResultsDialog = false },
        )
    }

    if (dohDialog) {
        TvOptionDialog(
            title = stringResource(R.string.settings_dns_over_https),
            options = DohProvider.entries.map {
                Triple(it.id, it == uiState.networkSettings.dohProvider) {
                    viewModel.setDohProvider(it)
                    dohDialog = false
                }
            },
            onDismiss = { dohDialog = false },
        )
    }

    if (showClearViewedConfirm) {
        TvConfirmDialog(
            title = stringResource(R.string.settings_clear_viewed_torrents),
            message = stringResource(R.string.settings_clear_viewed_torrents_dialog_text),
            confirmLabel = stringResource(
                R.string.settings_clear_viewed_torrents_dialog_button_clear,
            ),
            onConfirm = {
                viewModel.clearViewedTorrents()
                showClearViewedConfirm = false
            },
            onDismiss = { showClearViewedConfirm = false },
        )
    }
}

@Composable
private fun onOff(enabled: Boolean): String = stringResource(
    if (enabled) R.string.settings_dark_theme_on else R.string.settings_dark_theme_off,
)

@Composable
private fun DarkTheme.labelRes(): Int = when (this) {
    DarkTheme.On -> R.string.settings_dark_theme_on
    DarkTheme.Off -> R.string.settings_dark_theme_off
    DarkTheme.FollowSystem -> R.string.settings_dark_theme_follow_system
}

@Composable
private fun MaxNumResults.displayText(): String =
    if (isUnlimited()) {
        stringResource(R.string.settings_max_num_results_button_unlimited)
    } else {
        stringResource(R.string.settings_max_num_results_summary_format, n)
    }

/** Yes/no confirmation. */
@Composable
fun TvConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.tv.material3.Surface(
            shape = MaterialTheme.shapes.extraLarge,
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.tvCardSurface,
            ),
            modifier = Modifier.fillMaxWidth(0.6f).padding(vertical = 48.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(MaterialTheme.spaces.large)
                    .padding(TvFocusDefaults.Reserve),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.medium),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
                ) {
                    TvActionButton(onClick = onConfirm, strong = true) {
                        Text(text = confirmLabel)
                    }
                    TvActionButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(
                                R.string.settings_clear_viewed_torrents_dialog_button_cancel,
                            ),
                        )
                    }
                }
            }
        }
    }
}