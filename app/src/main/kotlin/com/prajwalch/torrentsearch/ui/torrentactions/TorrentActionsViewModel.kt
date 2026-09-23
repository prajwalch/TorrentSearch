package com.prajwalch.torrentsearch.ui.torrentactions

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

import java.io.OutputStream

sealed interface MagnetUriState {
    data object Loading : MagnetUriState

    data object Fetching : MagnetUriState

    data object Error : MagnetUriState

    data class Ready(val value: String) : MagnetUriState
}

sealed interface TorrentFileState {
    data object Downloading : TorrentFileState
    data class DownloadComplete(val fileName: String) : TorrentFileState
    data object DownloadFailed : TorrentFileState
    data object FileNotFound : TorrentFileState
    data object WritingContent : TorrentFileState
    data object WriteComplete : TorrentFileState
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

    private val _torrentFileState = MutableStateFlow<TorrentFileState?>(null)
    val torrentFileState = _torrentFileState.asStateFlow()

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


    fun downloadTorrentFile(magnetUri: String?) {
        _torrentFileState.value = TorrentFileState.Downloading

        viewModelScope.launch {
            val downloadResult = if (torrent.fileDownloadLink != null) {
                torrentFileDownloader.download(torrent.fileDownloadLink)
            } else {
                val magnetUri = requireNotNull(magnetUri)
                val infoHash = TorrentUtils.getInfoHashFromMagnetUri(magnetUri)
                torrentFileDownloader.tryDownloadUsingInfoHash(infoHash)
            }

            when (downloadResult) {
                TorrentFileDownloadResult.Failed -> {
                    _torrentFileState.value = TorrentFileState.DownloadFailed
                }

                TorrentFileDownloadResult.FileNotFound -> {
                    _torrentFileState.value = TorrentFileState.FileNotFound
                }

                is TorrentFileDownloadResult.Success -> {
                    pendingTorrentFile = downloadResult.content

                    val fileName = torrent.name.replace(" ", "_")
                    _torrentFileState.value = TorrentFileState.DownloadComplete(fileName)
                }
            }
        }
    }

    fun writeTorrentFileContent(outputStream: OutputStream) {
        viewModelScope.launch {
            _torrentFileState.value = TorrentFileState.WritingContent

            outputStream.use {
                val currentPendingFile = pendingTorrentFile ?: return@use

                withContext(Dispatchers.IO) {
                    currentPendingFile.let(it::write)
                }
            }

            _torrentFileState.value = TorrentFileState.WriteComplete
        }
    }

    fun resetTorrentFileState() {
        _torrentFileState.value = null
    }
}