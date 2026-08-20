package com.prajwalch.torrentsearch.ui.home

import android.content.res.Configuration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.home.component.AppBranding
import com.prajwalch.torrentsearch.ui.home.component.RecentSearchList
import com.prajwalch.torrentsearch.ui.home.component.SearchBox
import com.prajwalch.torrentsearch.ui.home.component.SearchProvidersNotEnabledMessage
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            AnimatedVisibility(uiState.settings.searchProvidersInitialized == false) {
                SearchProvidersNotEnabledMessage(
                    modifier = Modifier.padding(MaterialTheme.spaces.large),
                    onEnableRecommended = { viewModel.enableDefaultSearchProviders() },
                    onSkip = {
                        viewModel.skipDefaultSearchProviders()
                        onNavigateToSearchProviders()
                    },
                    onClose = { viewModel.skipDefaultSearchProviders() },
                )
            }

            Spacer(Modifier.height(MaterialTheme.spaces.extraLarge))

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
                    onCategorySelect = viewModel::setCategory,
                    suggestions = uiState.searchSuggestions,
                    onFilterSuggestions = viewModel::filterSearchSuggestions,
                )
            }

            AnimatedVisibility(uiState.recentSearches.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                        text = stringResource(R.string.home_title_recent_searches),
                    )

                    RecentSearchList(
                        queries = uiState.recentSearches,
                        onQueryClick = { onSearch(it, uiState.selectedCategory) },
                    )
                }
            }

            val configuration = LocalConfiguration.current
            val isInLandscapeMode = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isInLandscapeMode) {
                Spacer(Modifier.height(MaterialTheme.spaces.large))
            }
        }
    }
}

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
                    painter = painterResource(R.drawable.ic_star_filled),
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