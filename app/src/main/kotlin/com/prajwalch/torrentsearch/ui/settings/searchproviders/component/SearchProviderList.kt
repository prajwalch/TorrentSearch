package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderId
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
        items(items = searchProviders, key = { it.id }) {
            var showUnsafeReason by rememberSaveable { mutableStateOf<Int?>(null) }
            showUnsafeReason?.let { reasonResId ->
                SearchProviderUnsafeDetailsDialog(
                    onDismissRequest = { showUnsafeReason = null },
                    providerName = it.name,
                    url = it.url,
                    unsafeReason = stringResource(reasonResId),
                )
            }

            SearchProviderListItem(
                modifier = Modifier.animateItem(),
                name = it.name,
                url = it.url,
                supportedCategories = it.supportedCategories,
                type = it.type,
                safetyStatus = it.safetyStatus,
                protectionStatus = it.cloudflareProtectionStatus,
                enabled = it.isEnabled,
                onEnable = { enable -> onEnableSearchProvider(it.id, enable) },
                onUnlockProtection = {
                    onUnlockProtection(it.id, it.cloudflareSolverUrl ?: it.url)
                },
                onEditConfig = { onEditConfig(it.id) },
                onDeleteConfig = { onDeleteConfig(it.id) },
                onShowUnsafeReason = { reasonResId -> showUnsafeReason = reasonResId },
            )
        }
    }
}