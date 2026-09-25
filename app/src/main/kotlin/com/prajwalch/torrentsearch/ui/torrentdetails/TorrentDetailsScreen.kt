package com.prajwalch.torrentsearch.ui.torrentdetails

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.ui.component.NSFWBadge
import com.prajwalch.torrentsearch.ui.component.NoInternetConnectionState
import com.prajwalch.torrentsearch.ui.component.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.extension.copyText
import com.prajwalch.torrentsearch.ui.extension.openMagnetLink
import com.prajwalch.torrentsearch.ui.extension.startTextShareIntent
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.ui.torrentdetails.component.ActionButtonRow
import com.prajwalch.torrentsearch.ui.torrentdetails.component.DetailsUnavailableState
import com.prajwalch.torrentsearch.ui.torrentdetails.component.NsfwPosterImage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.PosterImage
import com.prajwalch.torrentsearch.ui.torrentdetails.component.ScreenshotImage
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
    modifier: Modifier = Modifier,
    viewModel: TorrentDetailsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current

    val linkCopiedMessage = stringResource(R.string.torrent_details_message_link_copied)
    val infoHashCopiedMessage = stringResource(R.string.torrent_details_message_info_hash_copied)

    var showTorrentClientNotFoundDialog by rememberSaveable { mutableStateOf(false) }
    if (showTorrentClientNotFoundDialog) {
        TorrentClientNotFoundDialog(
            onConfirmation = { showTorrentClientNotFoundDialog = false },
        )
    }

    val createTorrentFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(TorrentSearchConstants.MIME_TYPE_TORRENT),
    ) { fileUri ->
        fileUri
            ?.let(context.contentResolver::openOutputStream)
            ?.let(viewModel::writeTorrentFileContent)
    }

    LaunchedEffect(uiState.torrentFileState) {
        when (val torrentFileState = uiState.torrentFileState) {
            TorrentFileState.Idle -> {
                // No-op
            }

            is TorrentFileState.DownloadComplete -> {
                val fileName = torrentFileState.fileName
                createTorrentFileLauncher.launch(fileName)

                val message = resources.getString(R.string.torrent_status_file_download_complete)
                val saveToFileLabel = resources.getString(R.string.torrent_button_save_to_file)

                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = saveToFileLabel,
                    withDismissAction = true,
                )

                when (result) {
                    SnackbarResult.Dismissed -> viewModel.resetTorrentFileState()
                    SnackbarResult.ActionPerformed -> createTorrentFileLauncher.launch(fileName)
                }
            }

            TorrentFileState.Downloading -> {
                val message = resources.getString(R.string.torrent_status_file_downloading)
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long,
                )
            }

            TorrentFileState.DownloadFailed -> {
                val message = resources.getString(R.string.torrent_status_file_download_failed)
                snackbarHostState.showSnackbar(message)
            }

            TorrentFileState.FileNotFound -> {
                val message = resources.getString(R.string.torrent_status_file_not_found)
                snackbarHostState.showSnackbar(message)
            }

            TorrentFileState.WritingContent -> {
                val message = resources.getString(R.string.torrent_status_file_saving)
                snackbarHostState.showSnackbar(message)
            }

            TorrentFileState.WriteComplete -> {
                val message = resources.getString(R.string.torrent_status_file_saved)
                snackbarHostState.showSnackbar(message)

                viewModel.resetTorrentFileState()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TorrentDetailsScreenTopBar(
                onNavigateBack = onNavigateBack,
                onOpenPageLink = { uriHandler.openUri(viewModel.detailsPageUrl) },
                onSharePageLink = { context.startTextShareIntent(viewModel.detailsPageUrl) },
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
        AnimatedContent(
            modifier = Modifier.padding(innerPadding),
            targetState = uiState.detailsState
        ) { detailsState ->
            when (detailsState) {
                TorrentDetailsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                TorrentDetailsState.NoInternetConnection -> {
                    NoInternetConnectionState(
                        modifier = Modifier.fillMaxSize(),
                        onTryAgain = viewModel::loadDetails,
                    )
                }

                TorrentDetailsState.Unavailable -> {
                    DetailsUnavailableState(
                        modifier = Modifier.fillMaxSize(),
                        onTryAgain = viewModel::loadDetails,
                    )
                }

                is TorrentDetailsState.UnsupportedTorrentSite -> {
                    UnsupportedTorrentSiteState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.spaces.large),
                        host = detailsState.host,
                        onOpenInBrowser = { uriHandler.openUri(viewModel.detailsPageUrl) },
                    )
                }

                is TorrentDetailsState.SomethingWentWrong -> {
                    SomethingWentWrongState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.spaces.large),
                        message = detailsState.message,
                        onTryAgain = viewModel::loadDetails,
                    )
                }

                is TorrentDetailsState.Ready -> {
                    val torrentDetails = detailsState.details

                    TorrentDetailsScreenContent(
                        modifier = Modifier.fillMaxSize(),
                        details = torrentDetails,
                        onOpenMagnetLink = {
                            showTorrentClientNotFoundDialog =
                                !context.openMagnetLink(torrentDetails.magnetUri)
                        },
                        onDownloadTorrentFile = {
                            viewModel.downloadTorrentFile(
                                url = torrentDetails.fileDownloadLink,
                                infoHash = torrentDetails.infoHash,
                                torrentName = torrentDetails.name,
                            )
                        },
                        onCopyInfoHash = {
                            coroutineScope.launch {
                                clipboard.copyText(torrentDetails.infoHash)
                                snackbarHostState.showSnackbar(infoHashCopiedMessage)
                            }
                        },
                        isBookmarked = uiState.isBookmarked,
                        onToggleBookmark = { viewModel.toggleBookmark(it, torrentDetails) },
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshDetails,
                        providerName = viewModel.providerName,
                        blurNSFWImage = uiState.blurNSFWImages,
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
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TorrentDetailsScreenContent(
    details: TorrentDetails,
    onOpenMagnetLink: () -> Unit,
    onDownloadTorrentFile: () -> Unit,
    onCopyInfoHash: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: (Boolean) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    providerName: String,
    modifier: Modifier = Modifier,
    blurNSFWImage: Boolean = true,
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
        ) {
            HeaderSection(
                posterUrl = details.posterUrl,
                torrentName = details.name,
                onOpenMagnetLink = onOpenMagnetLink,
                onDownloadTorrentFile = onDownloadTorrentFile,
                isBookmarked = isBookmarked,
                onToggleBookmark = onToggleBookmark,
                isNSFW = details.isNSFW,
                blurNSFWImage = blurNSFWImage,
            )

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
                ScreenshotsSection(details.screenshotUrls)
            }

            details.description?.let {
                DescriptionSection(description = it, isNSFW = details.isNSFW)
            }

            Spacer(Modifier.height(MaterialTheme.spaces.large))
        }
    }
}

@Composable
private fun HeaderSection(
    posterUrl: String?,
    torrentName: String,
    onOpenMagnetLink: () -> Unit,
    onDownloadTorrentFile: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: (Boolean) -> Unit,
    isNSFW: Boolean,
    blurNSFWImage: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.spaces.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        posterUrl?.let {
            if (isNSFW) {
                NsfwPosterImage(url = it, initialRevealed = !blurNSFWImage)
            } else {
                PosterImage(url = it)
            }
        }

        Column {
            if (isNSFW) NSFWBadge()
            Text(
                text = torrentName,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        ActionButtonRow(
            onOpenMagnetLink = onOpenMagnetLink,
            onDownloadTorrentFile = onDownloadTorrentFile,
            isBookmarked = isBookmarked,
            onToggleBookmark = onToggleBookmark,
        )
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
    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.spaces.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        SectionTitle(stringResource(R.string.torrent_details_title_info))
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        SectionTitle(
            modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
            title = stringResource(R.string.torrent_details_title_screenshots),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
        ) {
            items(items = screenshotUrls, key = { it }) {
                ScreenshotImage(modifier = Modifier.animateItem(), url = it)
            }
        }
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    isNSFW: Boolean,
    modifier: Modifier = Modifier,
) {
    var showDescription by rememberSaveable(isNSFW) { mutableStateOf(!isNSFW) }

    Column(
        modifier = modifier
            .padding(horizontal = MaterialTheme.spaces.large)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(stringResource(R.string.torrent_details_title_description))

            if (isNSFW) {
                FilledTonalIconButton(onClick = { showDescription = !showDescription }) {
                    val iconResId = if (showDescription) {
                        R.drawable.ic_visibility_off
                    } else {
                        R.drawable.ic_visibility
                    }

                    Icon(
                        painter = painterResource(iconResId),
                        contentDescription = null,
                    )
                }
            }
        }

        Crossfade(showDescription) { shouldShowDescription ->
            if (shouldShowDescription) {
                TorrentDescription(description)
            } else {
                Text(
                    text = stringResource(R.string.torrent_details_message_description_hidden),
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
    )
}