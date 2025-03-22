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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    // FIXME: Make it scrollable.
    Row {
        for (contentType in ContentType.entries) {
            Text(
                text = contentType.toString(),
                modifier = Modifier.clickable(onClick = { onSelect(contentType) })
            )
            Spacer(Modifier.width(5.dp))
        }
    }
}

@Composable
fun TorrentListItem(torrent: Torrent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Text(torrent.name)
        TorrentMetadataInfo(torrent)
        HorizontalDivider()
    }
}

@Composable
fun TorrentMetadataInfo(torrent: Torrent) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Size ${torrent.size}")
        Text("↑ Seeds: ${torrent.seeds} ↓ Peers: ${torrent.peers}")
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