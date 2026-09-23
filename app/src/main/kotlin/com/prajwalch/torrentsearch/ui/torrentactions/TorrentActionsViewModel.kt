package com.prajwalch.torrentsearch.ui.torrentactions

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarkRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.GetMagnetUriResult
import com.prajwalch.torrentsearch.domain.TorrentFileDownloadResult
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader
import com.prajwalch.torrentsearch.domain.TorrentQueryService
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.util.TorrentUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

import java.io.OutputStream
import kotlin.time.Duration.Companion.seconds

sealed interface MagnetUriState {
    data object Loading : MagnetUriState

    data object Fetching : MagnetUriState

    data object Error : MagnetUriState

    data class Ready(val value: String) : MagnetUriState
}

@Stable
sealed interface TorrentFileLinkState {
    data object Preparing : TorrentFileLinkState
    data object WaitingForMagnetUri : TorrentFileLinkState
    data object Unavailable : TorrentFileLinkState
    data class Ready(val value: String) : TorrentFileLinkState
}

@Stable
sealed interface TorrentFileDownloadState {
    data object Downloading : TorrentFileDownloadState
    data class DownloadComplete(val fileName: String) : TorrentFileDownloadState
    data object DownloadFailed : TorrentFileDownloadState
    data object FileNotFound : TorrentFileDownloadState
    data object WritingContent : TorrentFileDownloadState
    data object WriteComplete : TorrentFileDownloadState
}

@KoinViewModel
class TorrentActionsViewModel(
    @InjectedParam private val torrent: Torrent,
    private val torrentQueryService: TorrentQueryService,
    private val bookmarkRepository: BookmarkRepository,
    private val torrentFileDownloader: TorrentFileDownloader,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val magnetUriState: StateFlow<MagnetUriState> = flow {
        when (val magnetUri = torrent.magnetUri) {
            is MagnetUri.Available -> {
                emit(MagnetUriState.Ready(magnetUri.value))
            }

            is MagnetUri.RequiresFetch -> {
                emit(MagnetUriState.Fetching)

                val result = torrentQueryService.getMagnetUri(
                    torrentId = torrent.id,
                    url = magnetUri.url,
                    providerName = torrent.providerName
                )

                when (result) {
                    is GetMagnetUriResult.Success -> emit(MagnetUriState.Ready(result.magnetUri))
                    is GetMagnetUriResult.Error -> emit(MagnetUriState.Error)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MagnetUriState.Loading,
    )

    val torrentFileLinkState: StateFlow<TorrentFileLinkState> = flow {
        if (torrent.fileDownloadLink != null) {
            emit(TorrentFileLinkState.Ready(torrent.fileDownloadLink))
        } else {
            val fromMagnetUri = magnetUriState.map {
                when (it) {
                    MagnetUriState.Loading, MagnetUriState.Fetching -> {
                        TorrentFileLinkState.WaitingForMagnetUri
                    }

                    MagnetUriState.Error -> TorrentFileLinkState.Unavailable

                    is MagnetUriState.Ready -> {
                        TorrentFileLinkState.Ready(createFallbackFileDownloadLink(it.value))
                    }
                }
            }

            emitAll(fromMagnetUri)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = TorrentFileLinkState.Preparing,
    )

    private val _torrentFileDownloadState = MutableStateFlow<TorrentFileDownloadState?>(null)
    val torrentFileDownloadState = _torrentFileDownloadState.asStateFlow()

    val isTorrentBookmarked: StateFlow<Boolean> =
        bookmarkRepository.getBookmarkIds()
            .map { torrent.id in it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

    val openTorrentDetailsInApp: StateFlow<Boolean> =
        settingsRepository.openTorrentDetailsInApp
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    private var pendingTorrentFile: ByteArray? = null

    private fun createFallbackFileDownloadLink(magnetUri: String): String {
        val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
        return "https://itorrents.net/torrent/${infoHash.uppercase()}.torrent"
    }

    fun toggleBookmark(bookmark: Boolean) {
        val currentMagnetUriState = magnetUriState.value
        if (currentMagnetUriState !is MagnetUriState.Ready) {
            return
        }

        viewModelScope.launch {
            val magnetUri = currentMagnetUriState.value

            if (bookmark) {
                bookmarkRepository.createAndAddBookmark(
                    torrentId = torrent.id,
                    name = torrent.name,
                    magnetUri = magnetUri,
                    size = torrent.size,
                    seeders = torrent.seeders,
                    peers = torrent.peers,
                    providerName = torrent.providerName,
                    uploadDate = torrent.uploadDate,
                    category = torrent.category,
                    descriptionPageUrl = torrent.descriptionPageUrl,
                    fileDownloadLink = torrent.fileDownloadLink,
                )
            } else {
                bookmarkRepository.deleteBookmarkById(torrent.id)
            }
        }
    }


    fun downloadTorrentFile(url: String) {
        _torrentFileDownloadState.value = TorrentFileDownloadState.Downloading

        viewModelScope.launch {
            when (val downloadResult = torrentFileDownloader.download(url)) {
                TorrentFileDownloadResult.Failed -> {
                    _torrentFileDownloadState.value = TorrentFileDownloadState.DownloadFailed
                }

                TorrentFileDownloadResult.FileNotFound -> {
                    _torrentFileDownloadState.value = TorrentFileDownloadState.FileNotFound
                }

                is TorrentFileDownloadResult.Success -> {
                    pendingTorrentFile = downloadResult.content

                    val fileName = torrent.name.replace(" ", "_")
                    _torrentFileDownloadState.value =
                        TorrentFileDownloadState.DownloadComplete(fileName)
                }
            }
        }
    }

    fun writeTorrentFileContent(outputStream: OutputStream) {
        viewModelScope.launch {
            _torrentFileDownloadState.value = TorrentFileDownloadState.WritingContent

            outputStream.use {
                val currentPendingFile = pendingTorrentFile ?: return@use

                withContext(Dispatchers.IO) {
                    currentPendingFile.let(it::write)
                }
            }

            _torrentFileDownloadState.value = TorrentFileDownloadState.WriteComplete
        }
    }

    fun resetTorrentFileState() {
        _torrentFileDownloadState.value = null
    }
}