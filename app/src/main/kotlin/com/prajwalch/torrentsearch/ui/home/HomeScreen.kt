package com.prajwalch.torrentsearch.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.home.component.AppBranding
import com.prajwalch.torrentsearch.ui.home.component.EnableSearchProvidersDialog
import com.prajwalch.torrentsearch.ui.home.component.ProvidersCheckNotificationBar
import com.prajwalch.torrentsearch.ui.home.component.RecentSearchesCard
import com.prajwalch.torrentsearch.ui.home.component.SearchBox
import com.prajwalch.torrentsearch.ui.theme.spaces

import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSearchHistory: () -> Unit,
    onBrowse: (Category) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSearch: (String, Category) -> Unit,
    onNavigateToSearchProviders: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val providersCheckState by viewModel.providersCheckState.collectAsStateWithLifecycle()

    if (uiState.settings.providersInitialized == false) {
        EnableSearchProvidersDialog(
            onDismiss = { viewModel.skipDefaultSearchProviders() },
            onEnableRecommended = { viewModel.enableDefaultSearchProviders() },
            onLetUserChoose = {
                onNavigateToSearchProviders()
                viewModel.skipDefaultSearchProviders()
            },
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        topBar = {
            HomeScreenTopBar(
                onNavigateToBookmarks = onNavigateToBookmarks,
                enableSearchHistory = uiState.settings.searchHistoryEnabled,
                onNavigateToSearchHistory = onNavigateToSearchHistory,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
    ) { innerPadding ->
        val pullToRefreshState = rememberPullToRefreshState()

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = false,
                    onRefresh = { viewModel.checkProviders() },
                    enabled = providersCheckState != ProvidersCheckState.Checking,
                ),
        ) {
            HomeScreenContent(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                uiState = uiState,
                onCategorySelect = { viewModel.setCategory(it) },
                onFilterSearchSuggestions = { viewModel.filterSearchSuggestions(it) },
                onSearch = onSearch,
                onBrowse = onBrowse,
                onHideRecentSearches = { viewModel.disableShowRecentSearches() },
            )

            AnimatedContent(
                modifier = Modifier.fillMaxWidth(),
                targetState = providersCheckState,
                transitionSpec = {
                    (fadeIn() + slideInVertically { -it }) togetherWith
                            (slideOutVertically { it } + fadeOut())
                },
                contentKey = { it.animationContentKey() },
            ) { targetState ->
                targetState?.let {
                    ProvidersCheckNotificationBar(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(MaterialTheme.spaces.large)
                            .fillMaxWidth(),
                        state = it,
                        onDismiss = { viewModel.finishProvidersCheck() },
                        onNavigateToSearchProviders = {
                            onNavigateToSearchProviders()
                            viewModel.finishProvidersCheck()
                        },
                        onRetry = { viewModel.checkProviders() },
                    )
                }
            }

            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
                isRefreshing = false,
            )
        }
    }
}

private fun ProvidersCheckState?.animationContentKey() =
    this?.let { ProvidersCheckState::class }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenTopBar(
    onNavigateToBookmarks: () -> Unit,
    enableSearchHistory: Boolean,
    onNavigateToSearchHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        actions = {
            IconButton(onClick = onNavigateToBookmarks) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmarks),
                    contentDescription = null,
                )
            }
            if (enableSearchHistory) {
                IconButton(onClick = onNavigateToSearchHistory) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = null,
                    )
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onCategorySelect: (Category) -> Unit,
    onFilterSearchSuggestions: (String) -> Unit,
    onSearch: (String, Category) -> Unit,
    onBrowse: (Category) -> Unit,
    onHideRecentSearches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showRecentSearches =
        uiState.settings.showRecentSearches && uiState.recentSearches.isNotEmpty()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        val topSpace by animateDpAsState(
            if (!showRecentSearches) {
                50.dp
            } else {
                MaterialTheme.spaces.large
            }
        )

        Spacer(Modifier.height(topSpace))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraLarge),
        ) {
            AppBranding()
            SearchBox(
                onSearch = { query -> onSearch(query, uiState.selectedCategory) },
                onBrowse = { onBrowse(uiState.selectedCategory) },
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = onCategorySelect,
                suggestions = uiState.searchSuggestions,
                onFilterSuggestions = onFilterSearchSuggestions
            )
        }

        AnimatedVisibility(showRecentSearches) {
            RecentSearchesCard(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                queries = uiState.recentSearches,
                onQueryClick = { query -> onSearch(query, uiState.selectedCategory) },
                onClose = onHideRecentSearches,
            )
        }

        Spacer(Modifier.height(MaterialTheme.spaces.large))
    }
}