package com.prajwalch.torrentsearch.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.ui.home.component.AppBranding
import com.prajwalch.torrentsearch.ui.home.component.SearchBox
import com.prajwalch.torrentsearch.ui.theme.spaces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSearchHistory: () -> Unit,
    onBrowse: (Category) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSearch: (String, Category) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        topBar = {
            HomeScreenTopBar(
                onNavigateToBookmarks = onNavigateToBookmarks,
                enableSearchHistory = uiState.searchHistoryEnabled,
                onNavigateToSearchHistory = onNavigateToSearchHistory,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(bottom = MaterialTheme.spaces.extraLarge)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // TODO: Use WindowSizeClass for better responsive layout.
            val configuration = LocalConfiguration.current
            val isPortraitMode = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

            if (isPortraitMode) {
                Spacer(Modifier.height(MaterialTheme.spaces.extraLarge * 5))
            }

            AppBranding()
            Spacer(Modifier.height(32.dp))

            SearchBox(
                onSearch = { query -> onSearch(query, uiState.selectedCategory) },
                onBrowse = { onBrowse(uiState.selectedCategory) },
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = viewModel::setCategory,
                histories = uiState.histories,
                onFilterSearchHistories = viewModel::filterSearchHistories,
            )
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