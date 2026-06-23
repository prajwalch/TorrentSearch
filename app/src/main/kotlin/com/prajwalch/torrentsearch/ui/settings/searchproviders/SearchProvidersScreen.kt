package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.component.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.settings.searchproviders.component.CloudflareChallengeBottomSheet
import com.prajwalch.torrentsearch.ui.settings.searchproviders.component.SearchProviderList
import com.prajwalch.torrentsearch.ui.theme.spaces

private typealias ProtectedProvider = Pair<SearchProviderId, String>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProvidersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddSearchProvider: () -> Unit,
    onNavigateToEditSearchProvider: (SearchProviderId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchProvidersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var protectedProvider by rememberSaveable { mutableStateOf<ProtectedProvider?>(null) }
    protectedProvider?.let { (searchProviderId, solverUrl) ->
        CloudflareChallengeBottomSheet(
            onDismiss = { protectedProvider = null },
            solverUrl = solverUrl,
            onChallengeSolved = {
                protectedProvider = null
                viewModel.markProviderAsUnlocked(searchProviderId)
            },
            webViewMaxHeight = 500.dp,
        )
    }

    val localResources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.protectionUpdateState) {
        when (val protectionUpdateState = uiState.protectionUpdateState) {
            ProtectionUpdateState.Idle -> {
                /* no op */
            }

            ProtectionUpdateState.Updating -> {
                snackbarHostState.showSnackbar(
                    message = localResources.getString(R.string.search_providers_state_updating_protection_status),
                    duration = SnackbarDuration.Indefinite,
                )
            }

//            is ProtectionUpdateState.Error -> {
//                val errorMessage = protectionUpdateState.message ?: "Unknown error occurred"
//                snackbarHostState.showSnackbar("Couldn't update protection status: $errorMessage")
//            }

            is ProtectionUpdateState.Complete -> {
                val message = localResources.getString(
                    R.string.search_providers_state_protection_status_update_complete,
                    protectionUpdateState.numUnlockedProviders,
                    protectionUpdateState.numLockedProviders,
                )
                snackbarHostState.showSnackbar(message)
                viewModel.resetProtectionUpdateState()
            }
        }
    }

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
                onUpdateProtectionStatus = viewModel::updateProtectionStatus,
                onResetToDefault = viewModel::resetEnabledSearchProvidersToDefault,
                subtitle = {
                    val searchProvidersSummary = stringResource(
                        R.string.settings_search_providers_summary_format,
                        uiState.enabledProvidersCount,
                        uiState.totalNumProviders,
                    )
                    Text(searchProvidersSummary)
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddSearchProvider) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            CategoryChipsRow(
                categories = Category.entries,
                selectedCategory = uiState.selectedCategory,
                onCategoryClick = viewModel::toggleCategory,
                contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
            )
            SearchProviderList(
                contentPadding = PaddingValues(
                    start = MaterialTheme.spaces.large,
                    top = MaterialTheme.spaces.large,
                    end = MaterialTheme.spaces.large,
                    bottom = 80.dp,
                ),
                searchProviders = uiState.searchProviders,
                onEnableSearchProvider = viewModel::enableSearchProvider,
                onUnlockProtection = { searchProviderId, solverUrl ->
                    protectedProvider = ProtectedProvider(searchProviderId, solverUrl)
                },
                onEditConfig = onNavigateToEditSearchProvider,
                onDeleteConfig = viewModel::deleteTorznabConfig,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchProvidersScreenTopBar(
    onNavigateBack: () -> Unit,
    onEnableAllSearchProviders: () -> Unit,
    onDisableAllSearchProviders: () -> Unit,
    onUpdateProtectionStatus: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column(verticalArrangement = Arrangement.Center) {
                Text(text = stringResource(R.string.search_providers_screen_title))
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    LocalTextStyle provides MaterialTheme.typography.labelMedium,
                ) {
                    subtitle?.let { it() }
                }
            }
        },
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

            Box {
                var showMoreMenu by rememberSaveable { mutableStateOf(false) }

                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = null,
                    )
                }
                TopBarMoreMenu(
                    expanded = showMoreMenu,
                    onDismiss = { showMoreMenu = false },
                    onUpdateProtectionStatus = {
                        onUpdateProtectionStatus()
                        showMoreMenu = false
                    },
                    onResetToDefault = {
                        onResetToDefault()
                        showMoreMenu = false
                    },
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TopBarMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onUpdateProtectionStatus: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundedDropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_safety_check),
                    contentDescription = stringResource(
                        R.string.search_providers_action_update_protection_status,
                    ),
                )
            },
            text = {
                Text(stringResource(R.string.search_providers_action_update_protection_status))
            },
            onClick = onUpdateProtectionStatus,
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_reset_settings),
                    contentDescription = stringResource(R.string.search_providers_action_reset),
                )
            },
            text = { Text(stringResource(R.string.search_providers_action_reset)) },
            onClick = onResetToDefault,
        )
    }
}