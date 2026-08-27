package com.prajwalch.torrentsearch.ui.torrentdetails

import android.content.res.Configuration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.ui.TorrentFileDownloadEffect
import com.prajwalch.torrentsearch.ui.component.NSFWBadge
import com.prajwalch.torrentsearch.ui.component.NoInternetConnectionState
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.ui.torrentdetails.component.CallToActionButton
import com.prajwalch.torrentsearch.ui.torrentdetails.component.CoverImage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.DetailsUnavailableState
import com.prajwalch.torrentsearch.ui.torrentdetails.component.NsfwPosterImage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.PosterImage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.Screenshots
import com.prajwalch.torrentsearch.ui.torrentdetails.component.SomethingWentWrongState
import com.prajwalch.torrentsearch.ui.torrentdetails.component.TorrentDescription
import com.prajwalch.torrentsearch.ui.torrentdetails.component.TorrentInfoCard
import com.prajwalch.torrentsearch.ui.torrentdetails.component.UnsupportedTorrentSiteState

import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailsScreen(
    onNavigateBack: () -> Unit,
    onOpenMagnetLink: (MagnetUri) -> Unit,
    onShareDetailsPageLink: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentDetailsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torrentFileDownloadState by viewModel.torrentFileDownloadState.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val linkCopiedMessage = stringResource(R.string.torrent_details_message_link_copied)
    val infoHashCopiedMessage = stringResource(R.string.torrent_details_message_info_hash_copied)

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
                        modifier = Modifier.fillMaxSize(),
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
                        onCopyInfoHash = {
                            coroutineScope.launch {
                                clipboard.copyText(torrentDetails.infoHash)
                                snackbarHostState.showSnackbar(infoHashCopiedMessage)
                            }
                        },
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshDetails,
                        blurNSFWImage = uiState.blurNSFWImages,
                        insetPadding = innerPadding,
                        contentPadding = PaddingValues(vertical = MaterialTheme.spaces.extraLarge),
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
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val isContentOverlapped = scrollBehavior.state.overlappedFraction > 0.01f
    val contentColor by animateColorAsState(
        if (isContentOverlapped) Color.Unspecified else MaterialTheme.colorScheme.onSurface
    )
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        navigationIconContentColor = contentColor,
        titleContentColor = contentColor,
        actionIconContentColor = contentColor,
    )

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        title = { Text(stringResource(R.string.torrent_details_screen_title)) },
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
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TorrentDetailsScreenContent(
    details: TorrentDetails,
    providerName: String,
    onOpenMagnetLink: () -> Unit,
    onDownloadTorrentFile: () -> Unit,
    onCopyInfoHash: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    blurNSFWImage: Boolean = true,
    insetPadding: PaddingValues = PaddingValues(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            details.posterUrl?.let { CoverImage(url = it) }

            Column(
                modifier = Modifier.padding(insetPadding + contentPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.extraLarge),
            ) {
                HeaderSection(
                    torrentName = details.name,
                    posterUrl = details.posterUrl,
                    isNSFW = details.isNSFW,
                    onOpenMagnetLink = onOpenMagnetLink,
                    onDownloadTorrentFile = onDownloadTorrentFile,
                    blurNSFWImage = blurNSFWImage,
                )

                HorizontalDivider()
                TorrentInfoSection(
                    size = details.size,
                    seeders = details.seeders,
                    peers = details.peers,
                    uploadDate = details.uploadDate,
                    category = details.category,
                    providerName = providerName,
                    uploader = details.uploader,
                    lastChecked = details.lastChecked,
                    infoHash = details.infoHash,
                    onCopyInfoHash = onCopyInfoHash,
                )

                if (details.screenshotUrls.isNotEmpty()) {
                    HorizontalDivider()
                    ScreenshotsSection(details.screenshotUrls)
                }

                details.description?.let {
                    HorizontalDivider()
                    DescriptionSection(description = it, isNSFW = details.isNSFW)
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    torrentName: String,
    posterUrl: String?,
    isNSFW: Boolean,
    onOpenMagnetLink: () -> Unit,
    onDownloadTorrentFile: () -> Unit,
    blurNSFWImage: Boolean,
    modifier: Modifier = Modifier,
) {
    val headerSectionContent = remember(torrentName, posterUrl, isNSFW) {
        movableContentOf {
            posterUrl?.let {
                if (isNSFW) {
                    NsfwPosterImage(url = it, initialRevealed = !blurNSFWImage)
                } else {
                    PosterImage(url = it)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large)) {
                Column {
                    if (isNSFW) NSFWBadge()
                    Text(
                        text = torrentName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                CallToActionButton(
                    onOpenMagnetLink = onOpenMagnetLink,
                    onDownloadTorrentFile = onDownloadTorrentFile,
                )
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isInPortraitMode = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    if (isInPortraitMode) {
        Column(
            modifier = modifier.padding(horizontal = MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        ) {
            headerSectionContent()
        }
    } else {
        Row(
            modifier = modifier.padding(horizontal = MaterialTheme.spaces.large),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        ) {
            headerSectionContent()
        }
    }
}

@Composable
private fun TorrentInfoSection(
    size: String?,
    seeders: UInt?,
    peers: UInt?,
    uploadDate: Instant?,
    category: Category?,
    providerName: String,
    uploader: String?,
    lastChecked: Instant?,
    infoHash: String,
    onCopyInfoHash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        modifier = modifier,
        title = { Text(stringResource(R.string.torrent_details_title_info)) },
        contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
    ) {
        TorrentInfoCard(
            size = size,
            seeders = seeders,
            peers = peers,
            uploadDate = uploadDate,
            category = category,
            provider = providerName,
            uploader = uploader,
            lastChecked = lastChecked,
            infoHash = infoHash,
            onCopyInfoHash = onCopyInfoHash,
        )
    }
}

@Composable
private fun ScreenshotsSection(screenshotUrls: List<String>, modifier: Modifier = Modifier) {
    DetailsSection(
        modifier = modifier,
        title = { Text(stringResource(R.string.torrent_details_title_screenshots)) },
    ) {
        Screenshots(
            urls = screenshotUrls,
            contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
        )
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    isNSFW: Boolean,
    modifier: Modifier = Modifier,
) {
    var descriptionVisible by rememberSaveable(isNSFW) { mutableStateOf(!isNSFW) }

    DetailsSection(
        modifier = modifier.animateContentSize(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.torrent_details_title_description))

                if (isNSFW) {
                    FilledTonalIconButton(onClick = { descriptionVisible = !descriptionVisible }) {
                        val iconId = if (descriptionVisible) {
                            R.drawable.ic_visibility_off
                        } else {
                            R.drawable.ic_visibility
                        }

                        Icon(
                            painter = painterResource(iconId),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
        contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
    ) {
        Crossfade(descriptionVisible) { showDescription ->
            if (showDescription) {
                TorrentDescription(description)
            } else {
                Text(
                    text = stringResource(R.string.torrent_details_message_description_hidden),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
private fun DetailsSection(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.primary,
            LocalTextStyle provides MaterialTheme.typography.titleMedium,
        ) {
            Box(modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large)) {
                title()
            }
        }
        Spacer(Modifier.height(MaterialTheme.spaces.large))
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}