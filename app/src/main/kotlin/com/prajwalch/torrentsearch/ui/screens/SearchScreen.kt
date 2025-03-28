package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.prajwalch.torrentsearch.data.ContentType
import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.ui.viewmodel.SearchScreenViewModel

@Composable
fun SearchScreen(viewModel: SearchScreenViewModel, onTorrentSelect: (Torrent) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    SearchBox(uiState.query, onQueryChange = viewModel::setQuery, onSubmit = viewModel::onSubmit)
    ContentTypeNavBar(uiState.contentType, onSelect = viewModel::setContentType)

    if (uiState.isLoading) {
        CircularProgressIndicator()
    }

    LazyColumn {
        items(uiState.results) { TorrentListItem(it, onClick = { onTorrentSelect(it) }) }
    }
}

@Composable
fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search...") },
            singleLine = true,
        )
        Button(onClick = onSubmit) { Text("Search") }
    }
}

@Composable
fun ContentTypeNavBar(activeContentType: ContentType, onSelect: (ContentType) -> Unit) {
    // FIXME: Make it scrollable.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        for (contentType in ContentType.entries) ContentTypeNavBarItem(
            label = contentType.toString(),
            isActive = activeContentType == contentType,
            onClick = { onSelect(contentType) }
        )
    }
}

@Composable
fun ContentTypeNavBarItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier.clickable(onClick = onClick),
        color = if (isActive) Color.Unspecified else Color.Gray,
        fontSize = 14.sp
    )
}

@Composable
fun TorrentListItem(torrent: Torrent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(10.dp)
            .clickable(onClick = onClick)
    ) {
        Text(torrent.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        TorrentMetadataInfo(torrent)
    }
    HorizontalDivider()
}

@Composable
fun TorrentMetadataInfo(torrent: Torrent) {
    val fontSize = 14.sp

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Size ${torrent.size}", fontSize = fontSize)
        Text("↑ Seeds: ${torrent.seeds} ↓ Peers: ${torrent.peers}", fontSize = fontSize)
    }
}