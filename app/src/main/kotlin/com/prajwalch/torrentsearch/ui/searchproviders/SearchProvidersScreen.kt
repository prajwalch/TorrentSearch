package com.prajwalch.torrentsearch.ui.searchproviders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.ui.component.FilterSearchBar
import com.prajwalch.torrentsearch.ui.searchproviders.component.CloudflareChallengeBottomSheet
import com.prajwalch.torrentsearch.ui.searchproviders.component.ResetToDefaultDialog
import com.prajwalch.torrentsearch.ui.searchproviders.component.SearchProviderFilterRow
import com.prajwalch.torrentsearch.ui.searchproviders.component.SearchProviderList
import com.prajwalch.torrentsearch.ui.searchproviders.component.SearchProvidersScreenTopBar
import com.prajwalch.torrentsearch.ui.theme.spaces

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

import org.koin.androidx.compose.koinViewModel

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private typealias ProtectedProvider = Pair<SearchProviderId, String>

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchProvidersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddSearchProvider: () -> Unit,
    onNavigateToEditSearchProvider: (SearchProviderId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchProvidersViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val localResources = LocalResources.current

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

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var protectedProvider by rememberSaveable { mutableStateOf<ProtectedProvider?>(null) }

    protectedProvider?.let { (searchProviderId, solverUrl) ->
        CloudflareChallengeBottomSheet(
            onDismiss = { protectedProvider = null },
            solverUrl = solverUrl,
            onChallengeSolved = {
                viewModel.markProviderAsUnlocked(searchProviderId)

                coroutineScope.launch {
                    delay(1.seconds)
                    bottomSheetState.hide()
                    protectedProvider = null
                }
            },
            webViewMaxHeight = 500.dp,
            sheetState = bottomSheetState,
        )
    }

    var showResetToDefaultDialog by rememberSaveable { mutableStateOf(false) }
    if (showResetToDefaultDialog) {
        ResetToDefaultDialog(
            onDismiss = { showResetToDefaultDialog = false },
            onReset = {
                viewModel.resetEnabledSearchProvidersToDefault()
                showResetToDefaultDialog = false
            },
        )
    }

    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState()

    if (showSearchBar) {
        LaunchedEffect(Unit) {
            snapshotFlow { textFieldState.text }
                .drop(1)
                .debounce(500.milliseconds)
                .collect { viewModel.filterSearchProviders(it.toString()) }
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
                onToggleSearchBar = { showSearchBar = !showSearchBar },
                onEnableAll = viewModel::enableAllSearchProviders,
                onDisableAll = viewModel::disableAllSearchProviders,
                onUpdateProtectionStatus = viewModel::updateProtectionStatus,
                onResetToDefault = { showResetToDefaultDialog = true },
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
            Column(modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large)) {
                AnimatedVisibility(visible = showSearchBar) {
                    FilterSearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spaces.small),
                        textFieldState = textFieldState,
                        placeholder = {
                            Text(stringResource(R.string.search_providers_search_hint))
                        },
                    )
                }

                SearchProviderFilterRow(
                    category = uiState.filter.category,
                    onCategorySelect = viewModel::toggleCategory,
                    protection = uiState.filter.protection,
                    onProtectionSelect = viewModel::toggleProviderProtection,
                )
            }

            SearchProviderList(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds(),
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