package com.prajwalch.torrentsearch.ui.tv.searchproviders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.CloudflareProtectionStatus
import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.domain.model.SearchProviderOrigin
import com.prajwalch.torrentsearch.ui.searchproviders.SearchProvidersViewModel
import com.prajwalch.torrentsearch.ui.tv.component.TvChip
import com.prajwalch.torrentsearch.ui.tv.component.TvChipRow
import com.prajwalch.torrentsearch.ui.tv.component.LocalTvContentFocusRequester
import com.prajwalch.torrentsearch.ui.tv.component.TvBadge
import com.prajwalch.torrentsearch.ui.tv.component.TvFocusDefaults
import com.prajwalch.torrentsearch.ui.tv.component.tvFocusRing
import com.prajwalch.torrentsearch.ui.tv.component.TvMenuItem
import com.prajwalch.torrentsearch.ui.tv.component.TvTopBar
import com.prajwalch.torrentsearch.ui.tv.component.tvListContentPadding
import com.prajwalch.torrentsearch.ui.tv.settings.TvConfirmDialog
import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface
import com.prajwalch.torrentsearch.ui.tv.theme.tvElevatedSurface

import org.koin.androidx.compose.koinViewModel

/**
 * TV search providers.
 *
 * Differences from the handheld list:
 *
 * - Each provider is one focusable row that toggles enable/disable on OK. The
 *   handheld row nests a `Switch` plus up to three more buttons inside a clickable
 *   item, which on a D-pad turns into a focus trap.
 * - The Torznab "edit / delete" menu was long-press-only on handheld; here it is a
 *   second, explicitly labelled row so it is reachable with OK.
 * - "Reset to default" is a confirmation dialog rather than a dropdown item.
 */
@Composable
fun TvSearchProvidersScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchProvidersViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetConfirm by remember { mutableStateOf(false) }

    // Screen entry point: focus lands on the bulk-action chips when this route opens.
    val entryFocusRequester = LocalTvContentFocusRequester.current

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(
            title = stringResource(R.string.search_providers_screen_title),
            subtitle = stringResource(
                R.string.settings_search_providers_summary_format,
                uiState.enabledProvidersCount,
                uiState.totalNumProviders,
            ),
        )

        // Bulk actions live in a chip row rather than the top bar: with four of
        // them the header had no room left for the title and rendered as
        // "Search provide...". This also matches how Search and Browse present
        // their filters.
        TvChipRow(
            chips = listOf(
                TvChip(
                    key = "enable-all",
                    label = stringResource(R.string.search_providers_action_enable_all),
                    onClick = viewModel::enableAllSearchProviders,
                ),
                TvChip(
                    key = "disable-all",
                    label = stringResource(R.string.search_providers_action_disable_all),
                    onClick = viewModel::disableAllSearchProviders,
                ),
                TvChip(
                    key = "update-protection",
                    label = stringResource(
                        R.string.search_providers_action_update_protection_status,
                    ),
                    onClick = viewModel::updateProtectionStatus,
                ),
                TvChip(
                    key = "reset",
                    label = stringResource(R.string.search_providers_action_reset),
                    onClick = { showResetConfirm = true },
                ),
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 4.dp),
            initialFocusRequester = entryFocusRequester,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = tvListContentPadding(),
            verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
        ) {
            items(uiState.searchProviders, key = { it.id }) { provider ->
                TvSearchProviderRow(
                    provider = provider,
                    onToggle = {
                        viewModel.enableSearchProvider(provider.id, !provider.isEnabled)
                    },
                    onEdit = { viewModel.enableSearchProvider(provider.id, provider.isEnabled) },
                    onDelete = { viewModel.deleteTorznabConfig(provider.id) },
                )
            }
        }
    }

    if (showResetConfirm) {
        TvConfirmDialog(
            title = stringResource(R.string.search_providers_reset_to_default_title),
            message = stringResource(R.string.search_providers_reset_to_default_text),
            confirmLabel = stringResource(
                R.string.search_providers_reset_to_default_button_confirm,
            ),
            onConfirm = {
                viewModel.resetEnabledSearchProvidersToDefault()
                showResetConfirm = false
            },
            onDismiss = { showResetConfirm = false },
        )
    }
}

@Composable
private fun TvSearchProviderRow(
    provider: SearchProviderInfo,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve / 3)) {
        Surface(
            onClick = onToggle,
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
                RoundedCornerShape(10.dp),
            ),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                containerColor = if (provider.isEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tvCardSurface
                },
                contentColor = if (provider.isEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                focusedContainerColor = if (provider.isEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tvElevatedSurface
                },
                focusedContentColor = if (provider.isEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                disabledContainerColor = MaterialTheme.colorScheme.tvCardSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                pressedContainerColor = MaterialTheme.colorScheme.tvElevatedSurface,
                pressedContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusRing(shape = RoundedCornerShape(10.dp)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (provider.origin == SearchProviderOrigin.Torznab) {
                    TvBadge(
                        text = stringResource(R.string.torznab),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                if (provider.cloudflareProtectionStatus == CloudflareProtectionStatus.Locked) {
                    TvBadge(
                        text = stringResource(R.string.search_error_kind_cloudflare_challenge),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Text(
                    text = if (provider.isEnabled) "ON" else "OFF",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Torznab configuration was long-press-only on handheld; surfaced explicitly.
        if (provider.origin == SearchProviderOrigin.Torznab) {
            Row(horizontalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve)) {
                TvMenuItem(
                    label = stringResource(R.string.search_providers_list_action_edit),
                    onClick = onEdit,
                )
                TvMenuItem(
                    label = stringResource(R.string.search_providers_list_action_delete),
                    onClick = onDelete,
                )
            }
        }
    }
}