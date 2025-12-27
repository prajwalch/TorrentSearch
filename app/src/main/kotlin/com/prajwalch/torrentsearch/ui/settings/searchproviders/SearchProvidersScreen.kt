package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.ui.components.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.components.BadgesRow
import com.prajwalch.torrentsearch.ui.components.CategoryBadge
import com.prajwalch.torrentsearch.ui.components.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.components.TextUrl
import com.prajwalch.torrentsearch.ui.components.TorznabBadge
import com.prajwalch.torrentsearch.ui.components.UnsafeBadge
import com.prajwalch.torrentsearch.ui.theme.spaces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProvidersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddSearchProvider: () -> Unit,
    onNavigateToEditSearchProvider: (SearchProviderId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchProvidersViewModel = hiltViewModel(),
) {
    val searchProvidersUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(modifier),
        topBar = {
            SearchProvidersScreenTopBar(
                onNavigateBack = onNavigateBack,
                onEnableAllSearchProviders = viewModel::enableAllSearchProviders,
                onDisableAllSearchProviders = viewModel::disableAllSearchProviders,
                onResetToDefault = viewModel::resetEnabledSearchProvidersToDefault,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddSearchProvider) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                )
            }
        },
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
                    SearchProviderType.Builtin -> {
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

                    SearchProviderType.Torznab -> {
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
                                viewModel.deleteTorznabConfig(id = searchProviderUiState.id)
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
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.search_providers_screen_title)) },
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        actions = {
            IconButton(onClick = onEnableAllSearchProviders) {
                Icon(
                    painter = painterResource(R.drawable.ic_select_all),
                    contentDescription = stringResource(
                        R.string.search_providers_action_enable_all,
                    ),
                )
            }
            IconButton(onClick = onDisableAllSearchProviders) {
                Icon(
                    painter = painterResource(R.drawable.ic_deselect_all),
                    contentDescription = stringResource(
                        R.string.search_providers_action_disable_all,
                    ),
                )
            }
            IconButton(onClick = onResetToDefault) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(
                        R.string.search_providers_action_reset,
                    ),
                )
            }
        },
        scrollBehavior = scrollBehavior,
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
    var showUnsafeReason by rememberSaveable { mutableStateOf<String?>(null) }

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
        category = specializedCategory,
        isTorznab = false,
        isUnsafe = safetyStatus.isUnsafe(),
        onShowUnsafeReason = {
            require(safetyStatus is SearchProviderSafetyStatus.Unsafe)
            showUnsafeReason = safetyStatus.reason
        }
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
                painter = painterResource(R.drawable.ic_warning),
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
            category = specializedCategory,
            isTorznab = true,
            isUnsafe = safetyStatus.isUnsafe(),
            onShowUnsafeReason = {},
        )

        TorznabSearchProviderMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            onEditConfiguration = {
                showMenu = false
                onEditConfig()
            },
            onDelete = {
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
    onEditConfiguration: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = MaterialTheme.spaces.large, y = 0.dp),
    ) {
        DropdownMenuItem(
            onClick = onEditConfiguration,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                )
            },
            text = {
                Text(text = stringResource(R.string.search_providers_list_action_edit))
            },
        )
        DropdownMenuItem(
            onClick = onDelete,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                )
            },
            text = {
                Text(text = stringResource(R.string.search_providers_list_action_delete))
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
    category: Category,
    isTorznab: Boolean,
    isUnsafe: Boolean,
    onShowUnsafeReason: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(text = name) },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.spaces.extraSmall,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                SearchProviderUrl(url = url)
                BadgesRow {
                    CategoryBadge(category = category)
                    if (isTorznab) TorznabBadge()
                    if (isUnsafe) UnsafeBadge()
                }
            }
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.spaces.extraSmall,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isUnsafe) {
                    FilledTonalIconButton(onClick = onShowUnsafeReason) {
                        Icon(
                            painter = painterResource(R.drawable.ic_question_mark),
                            contentDescription = null,
                        )
                    }
                }
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
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