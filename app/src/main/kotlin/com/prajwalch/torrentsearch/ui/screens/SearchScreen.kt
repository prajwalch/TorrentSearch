package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.components.TopSearchBar
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchScreenUIState
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTorrentSelect: (MagnetUri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLoading) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SearchScreenContent(
            uiState = uiState,
            onQueryChange = viewModel::setQuery,
            onCategoryChange = viewModel::setCategory,
            onSearch = viewModel::performSearch,
            onTorrentSelect = onTorrentSelect,
        )
    }
}

@Composable
private fun SearchScreenContent(
    uiState: SearchScreenUIState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onSearch: () -> Unit,
    onTorrentSelect: (MagnetUri) -> Unit,
) {
    TopSearchBar(
        modifier = Modifier.padding(vertical = 8.dp),
        query = uiState.query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
    )

    CategoryChipsRow(
        modifier = Modifier.padding(bottom = 8.dp),
        selectedCategory = uiState.category,
        onSelect = { newCategory ->
            if (uiState.category != newCategory) {
                onCategoryChange(newCategory)
                onSearch()
            }
        },
    )
    HorizontalDivider()

    if (uiState.isLoading) {
        LoadingIndicator(modifier = Modifier.fillMaxSize())
        return
    }

    if (uiState.isInternetError) {
        NoInternetConnectionMessage(
            modifier = Modifier.fillMaxSize(),
            onRetry = onSearch,
        )
        return
    }

    TorrentList(torrents = uiState.results, onTorrentSelect = onTorrentSelect)
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NoInternetConnectionMessage(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.no_internet_connection), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}