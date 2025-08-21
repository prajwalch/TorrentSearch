package com.prajwalch.torrentsearch.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.SettingsDialog
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProviderUiState
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProvidersViewModel

@Composable
fun SearchProvidersScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchProvidersViewModel,
    modifier: Modifier = Modifier,
) {
    val searchProvidersUiState by viewModel.searchProvidersUiState.collectAsStateWithLifecycle()
    var showAddIndexerDialog by rememberSaveable(searchProvidersUiState) { mutableStateOf(false) }

    if (showAddIndexerDialog) {
        AddNewSearchProviderDialog(
            onDismissRequest = { showAddIndexerDialog = false },
            onSave = { name, url, apiKey ->
                viewModel.addTorznabSearchProvider(
                    name = name,
                    url = url,
                    apiKey = apiKey,
                )
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchProvidersScreenTopBar(
                onNavigateBack = onNavigateBack,
                onEnableAllSearchProviders = viewModel::enableAllSearchProviders,
                onDisableAllSearchProviders = viewModel::disableAllSearchProviders,
                onResetToDefault = viewModel::resetEnabledSearchProvidersToDefault,
            )
        }
    ) { innerPadding ->
        SearchProviderList(
            modifier = Modifier.consumeWindowInsets(innerPadding),
            contentPadding = innerPadding,
            searchProviders = searchProvidersUiState,
            onProviderCheckedChange = viewModel::enableSearchProvider,
            toolBar = {
                TextButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { showAddIndexerDialog = true }
                ) {
                    Text(text = "Add Torznab compatible provider")
                }
                HorizontalDivider()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchProvidersScreenTopBar(
    onNavigateBack: () -> Unit,
    onEnableAllSearchProviders: () -> Unit,
    onDisableAllSearchProviders: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.setting_search_providers)) },
        navigationIcon = {
            NavigateBackIconButton(
                onClick = onNavigateBack,
                contentDescriptionId = R.string.button_go_to_settings_screen,
            )
        },
        actions = {
            IconButton(onClick = onEnableAllSearchProviders) {
                Icon(
                    painter = painterResource(R.drawable.ic_select_all),
                    contentDescription = stringResource(
                        R.string.button_enable_all_search_providers,
                    ),
                )
            }
            IconButton(onClick = onDisableAllSearchProviders) {
                Icon(
                    painter = painterResource(R.drawable.ic_deselect_all),
                    contentDescription = stringResource(
                        R.string.button_disable_all_search_providers,
                    ),
                )
            }
            IconButton(onClick = onResetToDefault) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(
                        R.string.button_reset_search_providers,
                    ),
                )
            }
        }
    )
}

@Composable
private fun SearchProviderList(
    searchProviders: List<SearchProviderUiState>,
    onProviderCheckedChange: (SearchProviderId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    toolBar: @Composable (() -> Unit)? = null,
) {
    var showUnsafeDetailsDialog by remember { mutableStateOf<SearchProviderUiState?>(null) }

    showUnsafeDetailsDialog?.let { searchProviderInfo ->
        SearchProviderUnsafeDetailsDialog(
            onDismissRequest = { showUnsafeDetailsDialog = null },
            providerName = searchProviderInfo.name,
            url = searchProviderInfo.url,
            // FIXME: This is poisonous code.
            unsafeReason = (searchProviderInfo.safetyStatus as SearchProviderSafetyStatus.Unsafe).reason,
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        toolBar?.let {
            item { it() }
        }
        items(items = searchProviders, key = { it.id }) { searchProvider ->
            SearchProviderListItem(
                modifier = Modifier.animateItem(),
                name = searchProvider.name,
                url = searchProvider.url,
                specializedCategory = searchProvider.specializedCategory,
                isUnsafe = searchProvider.safetyStatus.isUnsafe(),
                onShowUnsafeReason = { showUnsafeDetailsDialog = searchProvider },
                isTorznab = searchProvider.isTorznab,
                checked = searchProvider.enabled,
                onCheckedChange = { onProviderCheckedChange(searchProvider.id, it) },
            )
        }
    }
}

@Composable
private fun SearchProviderUnsafeDetailsDialog(
    onDismissRequest: () -> Unit,
    providerName: String,
    url: String,
    unsafeReason: String,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
            )
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = url,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        text = { Text(text = unsafeReason) },
    )
}

@Composable
private fun SearchProviderListItem(
    name: String,
    url: String,
    specializedCategory: Category,
    isUnsafe: Boolean,
    onShowUnsafeReason: () -> Unit,
    isTorznab: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = Modifier
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            )
            .then(modifier),
        overlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(category = specializedCategory)
                if (isTorznab) TorznabBadge()
                if (isUnsafe) UnsafeBadge(onClick = onShowUnsafeReason)
            }
        },
        headlineContent = { Text(text = name) },
        supportingContent = { SearchProviderUrl(url = url, isTorznab = isTorznab) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
private fun CategoryBadge(category: Category, modifier: Modifier = Modifier) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(text = category.name)
    }
}

@Composable
private fun TorznabBadge(modifier: Modifier = Modifier) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(text = stringResource(R.string.badge_torznab))
    }
}

@Composable
private fun UnsafeBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Badge(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(modifier),
    ) {
        Text(text = stringResource(R.string.badge_unsafe))
    }
}

@Composable
private fun SearchProviderUrl(url: String, isTorznab: Boolean, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val modifier = if (isTorznab) modifier else modifier.clickable { uriHandler.openUri(url) }

    if (url.isNotEmpty()) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = url.removePrefix("https://"),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            if (!isTorznab) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun AddNewSearchProviderDialog(
    onDismissRequest: () -> Unit,
    // (name?, url, API key)
    onSave: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var apiKey by rememberSaveable { mutableStateOf("") }

    val enableSaveButton by remember {
        derivedStateOf {
            name.isNotEmpty() && url.isNotEmpty() && apiKey.isNotEmpty()
        }
    }

    SettingsDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        titleId = R.string.title_new_indexer,
        confirmButton = {
            TextButton(
                enabled = enableSaveButton,
                onClick = { onSave(name, url, apiKey) },
            ) {
                Text(text = stringResource(R.string.button_save))
            }
        },
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(R.string.label_name)) },
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(text = stringResource(R.string.label_url)) },
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(text = stringResource(R.string.label_api_key)) },
                    singleLine = true,
                )
            }
        }
    }
}