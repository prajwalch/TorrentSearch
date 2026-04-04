package com.prajwalch.torrentsearch.torrentfiledownloader

import com.prajwalch.torrentsearch.data.repository.TorrentFileId
import com.prajwalch.torrentsearch.data.repository.TorrentRepository

import dagger.hilt.android.scopes.ViewModelScoped

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

sealed interface TorrentFileDownloadState {
    data object Empty : TorrentFileDownloadState

    data object Downloading : TorrentFileDownloadState

    data object Writing : TorrentFileDownloadState
}

sealed interface TorrentFileDownloadEvent {
    data class ReadyToWrite(val fileName: String) : TorrentFileDownloadEvent

    data class DownloadFailed(val message: String?) : TorrentFileDownloadEvent

    data object FileNotFound : TorrentFileDownloadEvent

    data object WriteSucceed : TorrentFileDownloadEvent

    data class WriteFailed(val message: String?) : TorrentFileDownloadEvent
}

@ViewModelScoped
class TorrentFileDownloader @Inject constructor(
    private val torrentsRepository: TorrentRepository,
) {
    private val _state = MutableStateFlow<TorrentFileDownloadState>(TorrentFileDownloadState.Empty)
    val state = _state.asStateFlow()

    private val _events = Channel<TorrentFileDownloadEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pendingFileId: TorrentFileId? = null

    suspend fun downloadFile(url: String, fileName: String) {
        reset()
        _state.value = TorrentFileDownloadState.Downloading

        try {
            val fileId = torrentsRepository.downloadTorrentFile(url)
            if (fileId == null) {
                _events.send(TorrentFileDownloadEvent.FileNotFound)
            } else {
                pendingFileId = fileId
                _events.send(TorrentFileDownloadEvent.ReadyToWrite(fileName))
            }
        } catch (e: CancellationException) {
            // Never catch this.
            throw e
        } catch (e: Throwable) {
            _events.send(TorrentFileDownloadEvent.DownloadFailed(e.message))
        } finally {
            _state.value = TorrentFileDownloadState.Empty
        }
    }

    suspend fun downloadFileFromInfoHash(infoHash: String, fileName: String) {
        val url = "https://itorrents.net/torrent/${infoHash.uppercase()}.torrent"
        downloadFile(url, fileName)
    }

    suspend fun writeFile(outputStream: OutputStream) {
        val pendingFileId = pendingFileId ?: return
        _state.value = TorrentFileDownloadState.Writing

        try {
            torrentsRepository.writeTorrentFile(pendingFileId, outputStream)
            _events.send(TorrentFileDownloadEvent.WriteSucceed)
        } catch (e: IOException) {
            _events.send(TorrentFileDownloadEvent.WriteFailed(e.message))
        } finally {
            reset()
        }
    }

    fun reset() {
        _state.value = TorrentFileDownloadState.Empty
        pendingFileId = null
    }
}