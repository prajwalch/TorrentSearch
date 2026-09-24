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
import kotlinx.coroutines.delay
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

@Stable
sealed interface MagnetUriState {
    data object Loading : MagnetUriState
    data object Error : MagnetUriState
    data class Ready(val value: String) : MagnetUriState
}

@Stable
sealed interface TorrentFileState {
    data object PreparingLink : TorrentFileState
    data object WaitingForMagnetUri : TorrentFileState
    data object LinkUnavailable : TorrentFileState
    data class LinkReady(val value: String) : TorrentFileState

    data object Downloading : TorrentFileState
    data object DownloadError : TorrentFileState
    data object FileNotFound : TorrentFileState
    data class DownloadComplete(val fileName: String) : TorrentFileState

    data object WritingContent : TorrentFileState
    data object ContentWriteComplete : TorrentFileState
}

@KoinViewModel
class TorrentActionsViewModel(
    @InjectedParam private val torrent: Torrent,
    private val torrentQueryService: TorrentQueryService,
    private val bookmarkRepository: BookmarkRepository,
    private val torrentFileDownloader: TorrentFileDownloader,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val torrentFileName = torrent.name.replace(" ", "_")

    val magnetUriState: StateFlow<MagnetUriState> = flow {
        when (val magnetUri = torrent.magnetUri) {
            is MagnetUri.Available -> emit(MagnetUriState.Ready(magnetUri.value))
            is MagnetUri.RequiresFetch -> {
                val result = torrentQueryService.getMagnetUri(
                    torrentId = torrent.id,
                    url = magnetUri.url,
                    providerName = torrent.providerName,
                )

                when (result) {
                    is GetMagnetUriResult.Success -> emit(MagnetUriState.Ready(result.magnetUri))
                    is GetMagnetUriResult.Error -> emit(MagnetUriState.Error)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = MagnetUriState.Loading,
    )

    private val _torrentFileState =
        MutableStateFlow<TorrentFileState>(TorrentFileState.PreparingLink)
    val torrentFileState: StateFlow<TorrentFileState> = _torrentFileState.asStateFlow()

    val isTorrentBookmarked: StateFlow<Boolean> =
        bookmarkRepository.getBookmarkIds()
            .map { torrent.id in it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = false,
            )

    val openTorrentDetailsInApp: StateFlow<Boolean> =
        settingsRepository.openTorrentDetailsInApp
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = true,
            )

    private var downloadedTorrentFileContent: ByteArray? = null

    init {
        prepareTorrentFileDownloadLink()
    }

    private fun prepareTorrentFileDownloadLink() {
        viewModelScope.launch {
            if (torrent.fileDownloadLink != null) {
                _torrentFileState.value = TorrentFileState.LinkReady(torrent.fileDownloadLink)
                return@launch
            }

            // Depend on magnet URI
            _torrentFileState.emitAll(
                magnetUriState.map {
                    when (it) {
                        MagnetUriState.Loading -> TorrentFileState.WaitingForMagnetUri
                        MagnetUriState.Error -> TorrentFileState.LinkUnavailable
                        is MagnetUriState.Ready -> {
                            TorrentFileState.LinkReady(createFallbackFileDownloadLink(it.value))
                        }
                    }
                }
            )
        }
    }

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
        _torrentFileState.value = TorrentFileState.Downloading

        viewModelScope.launch {
            _torrentFileState.value = when (val result = torrentFileDownloader.download(url)) {
                TorrentFileDownloadResult.Failed -> TorrentFileState.DownloadError
                TorrentFileDownloadResult.FileNotFound -> TorrentFileState.FileNotFound

                is TorrentFileDownloadResult.Success -> {
                    downloadedTorrentFileContent = result.content
                    TorrentFileState.DownloadComplete(torrentFileName)
                }
            }
        }
    }

    fun writeTorrentFileContent(outputStream: OutputStream) {
        _torrentFileState.value = TorrentFileState.WritingContent

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                outputStream.use {
                    val currentPendingFile = downloadedTorrentFileContent ?: return@use
                    currentPendingFile.let(it::write)
                }
            }

            _torrentFileState.value = TorrentFileState.ContentWriteComplete
            delay(1.seconds)
            _torrentFileState.value = TorrentFileState.DownloadComplete(torrentFileName)
        }
    }
}