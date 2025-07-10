package com.prajwalch.torrentsearch.ui.screens

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.ui.components.CategoryChipsRow
import com.prajwalch.torrentsearch.ui.components.TopSearchBar
import com.prajwalch.torrentsearch.ui.components.TorrentActionsBottomSheet
import com.prajwalch.torrentsearch.ui.components.TorrentList
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel

import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToSettings: () -> Unit,
    onDownloadTorrent: (MagnetUri) -> Unit,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLoading) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    selectedTorrent?.let { torrent ->
        val magnetLinkCopiedHint = stringResource(R.string.hint_magnet_link_copied)
        val descriptionPageUrlCopiedHint = stringResource(R.string.hint_description_page_url_copied)

        TorrentActionsBottomSheet(
            title = torrent.name,
            onDismissRequest = { selectedTorrent = null },
            onDownloadTorrent = { onDownloadTorrent(torrent.magnetUri()) },
            onCopyMagnetLink = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.magnetUri())
                    snackbarHostState.showSnackbar(magnetLinkCopiedHint)
                }
            },
            onShareMagnetLink = { onShareMagnetLink(torrent.magnetUri()) },
            onOpenDescriptionPage = { onOpenDescriptionPage(torrent.descriptionPageUrl) },
            onCopyDescriptionPageUrl = {
                coroutineScope.launch {
                    clipboard.copyText(text = torrent.descriptionPageUrl)
                    snackbarHostState.showSnackbar(descriptionPageUrlCopiedHint)
                }
            },
            onShareDescriptionPageUrl = { onShareDescriptionPageUrl(torrent.descriptionPageUrl) },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SearchScreenTopBar(
                modifier = Modifier.fillMaxWidth(),
                searchQuery = uiState.query,
                onSearchQueryChange = viewModel::setQuery,
                onSearch = viewModel::performSearch,
                onNavigateToSettings = onNavigateToSettings,
                category = uiState.category,
                onCategoryChange = viewModel::setCategory,
            )
        }
    ) { innerPadding ->
        SearchScreenContent(
            modifier = Modifier.padding(innerPadding),
            isLoading = uiState.isLoading,
            isInternetError = uiState.isInternetError,
            resultsNotFound = uiState.resultsNotFound,
            results = uiState.results,
            onRetry = viewModel::performSearch,
            onTorrentSelect = { selectedTorrent = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    category: Category,
    onCategoryChange: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowInsets = TopAppBarDefaults.windowInsets

    Column(
        modifier = Modifier
            .windowInsetsPadding(windowInsets)
            .then(modifier),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                shape = RoundedCornerShape(percent = 100),
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.button_go_to_settings_screen),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        CategoryChipsRow(
            selectedCategory = category,
            onSelect = { newCategory ->
                if (category != newCategory) {
                    onCategoryChange(newCategory)
                    onSearch()
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

@Composable
private fun SearchScreenContent(
    isLoading: Boolean,
    isInternetError: Boolean,
    resultsNotFound: Boolean,
    results: List<Torrent>,
    onRetry: () -> Unit,
    onTorrentSelect: (Torrent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.fillMaxSize())
        }

        if (isInternetError) {
            NoInternetConnectionMessage(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
        }

        if (resultsNotFound) {
            Spacer(Modifier.height(16.dp))
            ResultsNotFoundMessage()
        }

        if (results.isNotEmpty()) {
            TorrentList(torrents = results, onTorrentSelect = onTorrentSelect)
        }
    }
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
        Text(stringResource(R.string.msg_no_internet_connection), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.button_retry)) }
    }
}

@Composable
private fun ResultsNotFoundMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(58.dp),
            painter = painterResource(R.drawable.ic_sad),
            contentDescription = null,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.msg_no_results_found),
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Copies the text into the clipboard. */
private suspend fun Clipboard.copyText(text: String) {
    val clipData = ClipData.newPlainText(
        /* label = */
        null,
        /* text = */
        text,
    )
    val clipEntry = ClipEntry(clipData = clipData)

    this@copyText.setClipEntry(clipEntry = clipEntry)
}