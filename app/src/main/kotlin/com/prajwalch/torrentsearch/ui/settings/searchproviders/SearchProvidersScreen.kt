package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.ui.activityScopedViewModel
import com.prajwalch.torrentsearch.ui.components.BadgesRow
import com.prajwalch.torrentsearch.ui.components.CategoryBadge
import com.prajwalch.torrentsearch.ui.components.NavigateBackIconButton
import com.prajwalch.torrentsearch.ui.components.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.components.TextUrl
import com.prajwalch.torrentsearch.ui.components.TorznabBadge
import com.prajwalch.torrentsearch.ui.components.UnsafeBadge

@Composable
fun SearchProvidersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddSearchProvider: () -> Unit,
    onNavigateToEditSearchProvider: (SearchProviderId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = activityScopedViewModel<SearchProvidersViewModel>()
    val searchProvidersUiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchProvidersScreenTopBar(
                onNavigateBack = onNavigateBack,
                onEnableAllSearchProviders = viewModel::enableAllSearchProviders,
                onDisableAllSearchProviders = viewModel::disableAllSearchProviders,
                onResetToDefault = viewModel::resetEnabledSearchProvidersToDefault,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddSearchProvider) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                )
            }
        }
    ) { innerPadding ->
        SearchProviderList(
            modifier = Modifier.consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = innerPadding.calculateTopPadding(),
                end = 0.dp,
                bottom = 80.dp,
            ),
            searchProviders = searchProvidersUiState,
            listItem = { searchProviderUiState ->
                when (searchProviderUiState.type) {
                    is SearchProviderType.Builtin -> {
                        BuiltinSearchProviderListItem(
                            modifier = Modifier.animateItem(),
                            name = searchProviderUiState.name,
                            url = searchProviderUiState.url,
                            specializedCategory = searchProviderUiState.specializedCategory,
                            safetyStatus = searchProviderUiState.safetyStatus,
                            checked = searchProviderUiState.enabled,
                            onCheckedChange = {
                                viewModel.enableSearchProvider(
                                    providerId = searchProviderUiState.id,
                                    enable = it,
                                )
                            },
                        )
                    }

                    is SearchProviderType.Torznab -> {
                        TorznabSearchProviderListItem(
                            modifier = Modifier.animateItem(),
                            name = searchProviderUiState.name,
                            url = searchProviderUiState.url,
                            specializedCategory = searchProviderUiState.specializedCategory,
                            safetyStatus = searchProviderUiState.safetyStatus,
                            checked = searchProviderUiState.enabled,
                            onCheckedChange = {
                                viewModel.enableSearchProvider(
                                    providerId = searchProviderUiState.id,
                                    enable = it,
                                )
                            },
                            onEditConfig = {
                                onNavigateToEditSearchProvider(searchProviderUiState.id)
                            },
                            onDelete = {
                                viewModel.deleteTorznabSearchProvider(
                                    id = searchProviderUiState.id,
                                )
                            },
                        )
                    }
                }
            },
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
    listItem: @Composable (LazyItemScope.(SearchProviderUiState) -> Unit),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        items(items = searchProviders, key = { it.id }) {
            listItem(it)
        }
    }
}

@Composable
private fun BuiltinSearchProviderListItem(
    name: String,
    url: String,
    specializedCategory: Category,
    safetyStatus: SearchProviderSafetyStatus,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUnsafeReason by remember { mutableStateOf<String?>(null) }

    showUnsafeReason?.let { unsafeReason ->
        SearchProviderUnsafeDetailsDialog(
            onDismissRequest = { showUnsafeReason = null },
            providerName = name,
            url = url,
            unsafeReason = unsafeReason,
        )
    }

    SearchProviderListItem(
        modifier = modifier
            .clickable(role = Role.Switch) {
                onCheckedChange(!checked)
            },
        name = name,
        url = url,
        checked = checked,
        onCheckedChange = onCheckedChange,
        badges = {
            CategoryBadge(category = specializedCategory)

            if (safetyStatus is SearchProviderSafetyStatus.Unsafe) {
                UnsafeBadge(modifier = Modifier.clickable {
                    showUnsafeReason = safetyStatus.reason
                })
            }
        },
    )
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
private fun TorznabSearchProviderListItem(
    name: String,
    url: String,
    specializedCategory: Category,
    safetyStatus: SearchProviderSafetyStatus,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onEditConfig: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        SearchProviderListItem(
            modifier = Modifier.combinedClickable(
                interactionSource = null,
                indication = LocalIndication.current,
                onLongClick = { showMenu = true },
                onClick = { onCheckedChange(!checked) },
            ),
            name = name,
            url = url,
            checked = checked,
            onCheckedChange = onCheckedChange,
            badges = {
                CategoryBadge(category = specializedCategory)
                TorznabBadge()
                if (safetyStatus.isUnsafe()) UnsafeBadge()
            },
        )

        TorznabSearchProviderMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            onEditClick = {
                showMenu = false
                onEditConfig()
            },
            onDeleteClick = {
                showMenu = false
                onDelete()
            },
        )
    }
}

@Composable
private fun TorznabSearchProviderMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 16.dp, y = 0.dp),
    ) {
        DropdownMenuItem(
            onClick = onEditClick,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                )
            },
            text = {
                Text(text = stringResource(R.string.action_edit_configuration))
            },
        )
        DropdownMenuItem(
            onClick = onDeleteClick,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                )
            },
            text = {
                Text(text = stringResource(R.string.action_delete_search_provider))
            },
        )
    }
}

@Composable
private fun SearchProviderListItem(
    name: String,
    url: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    badges: @Composable (RowScope.() -> Unit) = {},
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(text = name) },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                SearchProviderUrl(url = url)
                BadgesRow(badges = badges)
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
private fun SearchProviderUrl(url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val isHttps = url.startsWith("https://")

    if (isHttps) {
        TextUrl(
            modifier = modifier,
            text = url.removePrefix("https://"),
            onClick = { uriHandler.openUri(url) },
        )
    } else {
        Text(
            modifier = modifier,
            text = url,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
    }
}