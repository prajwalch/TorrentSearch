package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.domain.model.CloudflareProtectionStatus
import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun SearchProviderList(
    searchProviders: List<SearchProviderInfo>,
    onEnableSearchProvider: (SearchProviderId, Boolean) -> Unit,
    onUnlockProtection: (id: SearchProviderId, url: String) -> Unit,
    onEditConfig: (SearchProviderId) -> Unit,
    onDeleteConfig: (SearchProviderId) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
        contentPadding = contentPadding,
    ) {
        items(items = searchProviders, key = { it.id }) { provider ->
            var showUnsafeReason by rememberSaveable { mutableStateOf<Int?>(null) }
            var showTorznabContextMenu by rememberSaveable { mutableStateOf(false) }

            showUnsafeReason?.let { reasonResId ->
                SearchProviderUnsafeDetailsDialog(
                    onDismissRequest = { showUnsafeReason = null },
                    providerName = provider.name,
                    url = provider.url,
                    unsafeReason = stringResource(reasonResId),
                )
            }

            val onClick: () -> Unit = {
                if (provider.cloudflareProtectionStatus == CloudflareProtectionStatus.Locked) {
                    onUnlockProtection(
                        provider.id,
                        provider.cloudflareSolverUrl ?: provider.url,
                    )
                } else {
                    onEnableSearchProvider(provider.id, !provider.isEnabled)
                }
            }
            // Long click handler for showing Torznab context menu.
            val longClickHandler: (() -> Unit)? = when (provider.type) {
                // Disable it for builtin providers.
                SearchProviderType.Builtin -> null
                SearchProviderType.Torznab -> ({ showTorznabContextMenu = true })
            }
            val clickableModifier = Modifier.combinedClickable(
                interactionSource = null,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = longClickHandler,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            ) {
                SearchProviderListItem(
                    modifier = Modifier
                        .clip(shape = MaterialTheme.shapes.large)
                        .then(clickableModifier),
                    name = provider.name,
                    url = provider.url,
                    supportedCategories = provider.supportedCategories,
                    type = provider.type,
                    safetyStatus = provider.safetyStatus,
                    protectionStatus = provider.cloudflareProtectionStatus,
                    enabled = provider.isEnabled,
                    onEnable = { enable -> onEnableSearchProvider(provider.id, enable) },
                    onUnlockProtection = {
                        onUnlockProtection(
                            provider.id,
                            provider.cloudflareSolverUrl ?: provider.url
                        )
                    },
                    onShowUnsafeReason = { reasonResId -> showUnsafeReason = reasonResId },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
                TorznabContextMenu(
                    expanded = showTorznabContextMenu,
                    onDismiss = { showTorznabContextMenu = false },
                    onEditConfiguration = {
                        onEditConfig(provider.id)
                        showTorznabContextMenu = false
                    },
                    onDelete = {
                        onDeleteConfig(provider.id)
                        showTorznabContextMenu = false
                    },
                )
            }
        }
    }
}