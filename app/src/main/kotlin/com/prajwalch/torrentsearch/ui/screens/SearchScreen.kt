package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.R

import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.ui.components.CategoryNavBar
import com.prajwalch.torrentsearch.ui.components.SearchBox
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchScreenViewModel

@Composable
fun SearchScreen(viewModel: SearchScreenViewModel, onTorrentSelect: (Torrent) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    SearchBox(uiState.query, onQueryChange = viewModel::setQuery, onSubmit = viewModel::onSubmit)
    CategoryNavBar(uiState.category, onSelect = viewModel::setCategory)
    HorizontalDivider()

    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.isInternetError) {
        NoInternetConnectionMessage(onRetry = viewModel::onSubmit)
        return
    }

    TorrentList(uiState.results, onClick = onTorrentSelect)
}

@Composable
fun NoInternetConnectionMessage(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.no_internet_connection), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}