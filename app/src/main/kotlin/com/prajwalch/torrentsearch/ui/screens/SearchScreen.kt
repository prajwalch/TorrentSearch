package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.ui.components.ContentTypeNavBar
import com.prajwalch.torrentsearch.ui.components.SearchBox
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchScreenViewModel

@Composable
fun SearchScreen(viewModel: SearchScreenViewModel, onTorrentSelect: (Torrent) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    SearchBox(uiState.query, onQueryChange = viewModel::setQuery, onSubmit = viewModel::onSubmit)
    ContentTypeNavBar(uiState.contentType, onSelect = viewModel::setContentType)
    HorizontalDivider()

    if (uiState.isLoading) {
        CircularProgressIndicator()
    }

    TorrentList(uiState.results, onClick = onTorrentSelect)
}
