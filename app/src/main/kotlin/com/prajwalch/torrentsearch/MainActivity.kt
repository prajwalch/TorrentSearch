package com.prajwalch.torrentsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

@Composable
fun App() {
    TorrentSearchTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                MainLayout()
            }
        }
    }
}

@Composable
fun MainLayout() {
    val searchEngine = remember { SearchEngine() }
    var query by remember { mutableStateOf("") }
    var contentType by remember { mutableStateOf(ContentType.All) }

    SearchBox(onSubmit = { query = it })
    ContentTypeNavBar(onSelect = { contentType = it })

    val torrents = if (query.isEmpty()) {
        emptyList()
    } else {
        searchEngine.search(query, contentType).orEmpty()
    }

    for (torrent in torrents) {
        TorrentListItem(torrent, onClick = {})
    }
}

@Composable
fun SearchBox(onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    var value by remember { mutableStateOf("") }

    Row(modifier) {
        TextField(value = value, onValueChange = { value = it })
        Spacer(modifier = Modifier.width(10.dp))
        Button(onClick = { onSubmit(value) }) { Text("Search") }
    }
}

@Composable
fun ContentTypeNavBar(onSelect: (ContentType) -> Unit) {
    var activeContentType by remember { mutableStateOf(ContentType.All) }

    // FIXME: Make it scrollable.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        for (contentType in ContentType.entries) ContentTypeNavBarItem(
            label = contentType.toString(),
            isActive = activeContentType == contentType,
            onClick = {
                activeContentType = contentType
                onSelect(contentType)
            }
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

@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun MainPreview() {
    App()
}