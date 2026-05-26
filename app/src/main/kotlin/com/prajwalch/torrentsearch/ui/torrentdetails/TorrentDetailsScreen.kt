package com.prajwalch.torrentsearch.ui.torrentdetails

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
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
import com.prajwalch.torrentsearch.ui.component.BadgeRow
import com.prajwalch.torrentsearch.ui.component.NSFWBadge
import com.prajwalch.torrentsearch.ui.component.NoInternetConnectionState
import com.prajwalch.torrentsearch.ui.component.SearchProviderBadge
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.ui.torrentdetails.component.CallToActionButton
import com.prajwalch.torrentsearch.ui.torrentdetails.component.DetailsUnavailableState
import com.prajwalch.torrentsearch.ui.torrentdetails.component.HeroBackgroundImage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.MediaPoster
import com.prajwalch.torrentsearch.ui.torrentdetails.component.NsfwMediaPoster
import com.prajwalch.torrentsearch.ui.torrentdetails.component.ScreenShots
import com.prajwalch.torrentsearch.ui.torrentdetails.component.SomethingWentWrongState
import com.prajwalch.torrentsearch.ui.torrentdetails.component.TorrentDescription
import com.prajwalch.torrentsearch.ui.torrentdetails.component.TorrentInfo
import com.prajwalch.torrentsearch.ui.torrentdetails.component.UnsupportedTorrentSiteState

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailsScreen(
    onNavigateBack: () -> Unit,
    onOpenMagnetLink: (MagnetUri) -> Unit,
    onShareDetailsPageLink: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torrentFileDownloadState by viewModel.torrentFileDownloadState.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val linkCopiedMessage = stringResource(R.string.torrent_details_message_link_copied)

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
                onCopyPageLink = {
                    coroutineScope.launch {
                        clipboard.copyText(viewModel.detailsPageUrl)
                        snackbarHostState.showSnackbar(linkCopiedMessage)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        AnimatedContent(targetState = uiState.state) { contentState ->
            when (contentState) {
                TorrentDetailsState.Loading -> {
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                TorrentDetailsState.NoInternetConnection -> {
                    NoInternetConnectionState(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        onTryAgain = viewModel::loadDetails,
                    )
                }

                TorrentDetailsState.Unavailable -> {
                    DetailsUnavailableState(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        onTryAgain = viewModel::loadDetails,
                    )
                }

                is TorrentDetailsState.UnsupportedTorrentSite -> {
                    UnsupportedTorrentSiteState(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.spaces.large),
                        host = contentState.host,
                        onOpenInBrowser = { uriHandler.openUri(viewModel.detailsPageUrl) },
                    )
                }

                is TorrentDetailsState.SomethingWentWrong -> {
                    SomethingWentWrongState(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.spaces.large),
                        message = contentState.message,
                        onTryAgain = viewModel::loadDetails,
                    )
                }

                is TorrentDetailsState.Available -> {
                    val torrentDetails = contentState.details

                    TorrentDetailsScreenContent(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(vertical = MaterialTheme.spaces.large),
                        details = torrentDetails,
                        providerName = viewModel.providerName,
                        onOpenMagnetLink = { onOpenMagnetLink(torrentDetails.magnetUri) },
                        onDownloadTorrentFile = {
                            if (torrentDetails.fileDownloadLink != null) {
                                viewModel.downloadTorrentFile(
                                    url = torrentDetails.fileDownloadLink,
                                    fileName = torrentDetails.name,
                                )
                            } else {
                                viewModel.downloadTorrentFileFromInfoHash(
                                    infoHash = torrentDetails.infoHash,
                                    fileName = torrentDetails.name,
                                )
                            }
                        },
                        blurNSFWImages = uiState.blurNSFWImages,
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
    onCopyPageLink: () -> Unit,
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
            IconButton(onClick = onCopyPageLink) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = stringResource(R.string.torrent_details_action_copy_link),
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
    providerName: String,
    onOpenMagnetLink: () -> Unit,
    onDownloadTorrentFile: () -> Unit,
    modifier: Modifier = Modifier,
    blurNSFWImages: Boolean = true,
) {
    var revealImage by rememberSaveable(blurNSFWImages) {
        mutableStateOf(!details.isNSFW || !blurNSFWImages)
    }
    var showTapToRevealHint by rememberSaveable { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        details.posterUrl?.let {
            HeroBackgroundImage(
                url = it,
                revealed = revealImage,
            )
        }

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraLarge),
        ) {
            val horizontalPaddingModifier = Modifier.padding(
                horizontal = MaterialTheme.spaces.large,
            )

            details.posterUrl?.let {
                val alignModifier = Modifier.align(Alignment.CenterHorizontally)

                if (details.isNSFW) {
                    NsfwMediaPoster(
                        modifier = Modifier.then(alignModifier),
                        url = it,
                        onToggleReveal = {
                            showTapToRevealHint = false
                            revealImage = !revealImage
                        },
                        revealed = revealImage,
                        showTapToRevealHint = showTapToRevealHint,
                    )
                } else {
                    MediaPoster(modifier = Modifier.then(alignModifier), url = it)
                }
            }

            Column(modifier = Modifier.then(horizontalPaddingModifier)) {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                BadgeRow {
                    SearchProviderBadge(providerName)
                    if (details.isNSFW) NSFWBadge()
                }
            }

            CallToActionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(horizontalPaddingModifier),
                onOpenMagnetLink = onOpenMagnetLink,
                onDownloadTorrentFile = onDownloadTorrentFile,
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
            HorizontalDivider()

            if (details.screenshotUrls.isNotEmpty()) {
                ScreenShots(
                    modifier = Modifier.fillMaxWidth(),
                    urls = details.screenshotUrls,
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large)
                )
                HorizontalDivider()
            }

            TorrentDescription(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(horizontalPaddingModifier),
                description = details.description,
            )
        }
    }
}