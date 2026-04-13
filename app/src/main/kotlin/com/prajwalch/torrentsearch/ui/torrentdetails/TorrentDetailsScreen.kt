package com.prajwalch.torrentsearch.ui.torrentdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.ui.TorrentFileDownloadEffect
import com.prajwalch.torrentsearch.ui.component.ArrowBackIconButton
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.ui.torrentdetails.component.CallToActionButton
import com.prajwalch.torrentsearch.ui.torrentdetails.component.HeroBackgroundImage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.LoadFailedMessage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.MediaPoster
import com.prajwalch.torrentsearch.ui.torrentdetails.component.ScreenShots
import com.prajwalch.torrentsearch.ui.torrentdetails.component.TorrentDescription
import com.prajwalch.torrentsearch.ui.torrentdetails.component.TorrentInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailsScreen(
    onNavigateBack: () -> Unit,
    onDownloadTorrent: (MagnetUri) -> Unit,
    onShareDetailsPageLink: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torrentFileDownloadState by viewModel.torrentFileDownloadState.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current
//    val clipboard = LocalClipboard.current

//    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    TorrentFileDownloadEffect(
        onWrite = viewModel::writeTorrentFile,
        state = torrentFileDownloadState,
        events = viewModel.torrentFileDownloadEvents,
        snackbarHostState = snackbarHostState,
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TorrentDetailsScreenTopBar(
                onNavigateBack = onNavigateBack,
                onOpenPageLink = { uriHandler.openUri(viewModel.detailsPageUrl) },
                onSharePageLink = { onShareDetailsPageLink(viewModel.detailsPageUrl) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val innerPaddingModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (val uiState = uiState) {
            is TorrentDetailsUiState.LoadFailed -> {
                LoadFailedMessage(
                    modifier = innerPaddingModifier,
                    message = uiState.error.displayName(),
                )
            }

            TorrentDetailsUiState.Loading -> {
                Box(
                    modifier = innerPaddingModifier,
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is TorrentDetailsUiState.Ready -> {
                val torrentDetails = uiState.details
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    torrentDetails.posterUrl?.let { HeroBackgroundImage(it) }

                    TorrentDetailsScreenContent(
                        modifier = innerPaddingModifier,
                        details = torrentDetails,
//                        onOpenUrl = { uriHandler.openUri(viewModel.detailsPageUrl) },
//                        onCopyUrl = {
//                            coroutineScope.launch {
//                                clipboard.copyText(viewModel.detailsPageUrl)
//                                snackbarHostState.showSnackbar(linkCopiedMessage)
//                            }
//                        },
                        onOpenMagnet = { onDownloadTorrent(torrentDetails.magnetUri) },
                        onDownloadTorrent = {
                            val torrentFileName = torrentDetails.name.replace(' ', '-')

                            if (torrentDetails.fileDownloadLink != null) {
                                viewModel.downloadTorrentFile(
                                    url = torrentDetails.fileDownloadLink,
                                    fileName = torrentFileName,
                                )
                            } else {
                                viewModel.downloadTorrentFileFromInfoHash(
                                    infoHash = torrentDetails.infoHash,
                                    fileName = torrentFileName,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TorrentDetailsScreenTopBar(
    onNavigateBack: () -> Unit,
    onOpenPageLink: () -> Unit,
    onSharePageLink: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = { ArrowBackIconButton(onClick = onNavigateBack) },
        title = {},
        actions = {
            IconButton(onClick = onOpenPageLink) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_browser),
                    contentDescription = stringResource(R.string.torrent_details_action_open_link),
                )
            }
            IconButton(onClick = onSharePageLink) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.torrent_details_action_share_link),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TorrentDetailsScreenContent(
    details: TorrentDetails,
    onOpenMagnet: () -> Unit,
    onDownloadTorrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        val horizontalPaddingModifier = Modifier.padding(
            horizontal = MaterialTheme.spaces.large,
        )

        details.posterUrl?.let {
            MediaPoster(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                url = it,
            )
        }
        Text(
            modifier = Modifier.then(horizontalPaddingModifier),
            text = details.name,
            style = MaterialTheme.typography.titleLarge,
        )
        CallToActionButton(
            modifier = Modifier
                .fillMaxWidth()
                .then(horizontalPaddingModifier),
            onOpenMagnet = onOpenMagnet,
            onDownloadTorrent = onDownloadTorrent,
        )
        HorizontalDivider()
        TorrentInfo(
            modifier = Modifier
                .fillMaxWidth()
                .then(horizontalPaddingModifier),
            size = details.size,
            seeders = details.seeders,
            peers = details.peers,
            uploadDate = details.uploadDate,
            category = details.category,
            uploader = details.uploader,
            lastChecked = details.lastChecked,
            infoHash = details.infoHash,
        )
        if (details.screenshotUrls.isNotEmpty()) {
            ScreenShots(
                modifier = Modifier.fillMaxWidth(),
                urls = details.screenshotUrls,
                onScreenshotClick = {},
                contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large)
            )
        }
        HorizontalDivider()
        TorrentDescription(
            modifier = Modifier
                .fillMaxWidth()
                .then(horizontalPaddingModifier),
            description = details.description,
        )
    }
}

@Composable
private fun LoadError.displayName(): String {
    val resId = when (this) {
        LoadError.UnsupportedSearchProvider -> R.string.torrent_details_error_unsupported_provider
        LoadError.DetailsNotFound -> R.string.torrent_details_error_not_found
    }

    return stringResource(resId)
}