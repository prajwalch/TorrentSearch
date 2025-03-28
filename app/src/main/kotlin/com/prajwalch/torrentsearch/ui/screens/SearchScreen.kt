package com.prajwalch.torrentsearch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
    HorizontalDivider()

    if (uiState.isLoading) {
        CircularProgressIndicator()
    }

    LazyColumn {
        items(uiState.results) { TorrentListItem(it, onClick = { onTorrentSelect(it) }) }
    }
}

@Composable
fun SearchBox(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    val colors = TextFieldDefaults.colors(
        unfocusedContainerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent
    )

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear search query"
                )
            }
        },
        singleLine = true,
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        colors = colors
    )
}

@Composable
fun ContentTypeNavBar(activeContentType: ContentType, onSelect: (ContentType) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(ContentType.entries.toList()) { index, contentType ->
            // TODO: This is hack to add space between items.
            //       horizontalArrangement is not working.
            if (index != 0) {
                Spacer(Modifier.width(10.dp))
            }

            ContentTypeNavBarItem(
                label = contentType.toString(),
                isActive = activeContentType == contentType,
                onClick = { onSelect(contentType) }
            )
        }
    }
}

@Composable
fun ContentTypeNavBarItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (isActive) Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Selected content type",
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
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