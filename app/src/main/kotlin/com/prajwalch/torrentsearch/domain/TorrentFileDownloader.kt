package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.data.repository.TorrentFileId
import com.prajwalch.torrentsearch.data.repository.TorrentFileRepository

import dagger.hilt.android.scopes.ViewModelScoped

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/**
 * Represents current state of file downloader.
 */
sealed interface TorrentFileDownloadState {
    /**
     * Downloading is not started yet.
     */
    data object Idle : TorrentFileDownloadState

    /**
     * Downloader is currently downloading a file.
     */
    data object Downloading : TorrentFileDownloadState

    /**
     * Downloader is currently writing content of the file.
     */
    data object Writing : TorrentFileDownloadState
}

/**
 * Represents a one-time event that can occur while downloading a file.
 */
sealed interface TorrentFileDownloadEvent {
    /**
     * File was downloaded successfully and content is ready to be written.
     */
    data class ReadyToWrite(val fileName: String) : TorrentFileDownloadEvent

    /**
     * File couldn't be downloaded successfully.
     *
     * The can happen when such as timeout occurs during connection establish,
     * host is unreachable, etc.
     */
    data class DownloadFailed(val message: String?) : TorrentFileDownloadEvent

    /**
     * File not found on the remote host.
     *
     * It indicates that the remote host is reachable but the file is no longer
     * available for downloading.
     */
    data object FileNotFound : TorrentFileDownloadEvent

    /**
     * Content of the file written successfully to out stream.
     */
    data object WriteSucceed : TorrentFileDownloadEvent

    /**
     * Content of the file couldn't be written successfully.
     *
     * This can happen when permission is denied.
     */
    data class WriteFailed(val message: String?) : TorrentFileDownloadEvent
}

/**
 * Manages and handles torrent file downloading related task.
 */
@ViewModelScoped
class TorrentFileDownloader @Inject constructor(
    private val torrentFileRepository: TorrentFileRepository,
) {
    /**
     * The internal, mutable source of truth for the downloader state.
     */
    private val _state = MutableStateFlow<TorrentFileDownloadState>(TorrentFileDownloadState.Idle)

    /**
     * The publicly observable, read-only state of the downloader.
     */
    val state = _state.asStateFlow()

    /**
     * The internal, mutable source for emitting one-time download events.
     */
    private val _events = Channel<TorrentFileDownloadEvent>(Channel.BUFFERED)

    /**
     * The publicly observable, read-only events of the downloader.
     */
    val events = _events.receiveAsFlow()

    /**
     * ID of the file whose content is yet to be written.
     */
    private var pendingFileId: TorrentFileId? = null

    /**
     * Downloads a torrent file from the given URL.
     */
    suspend fun download(url: String, fileName: String) {
        resetState()

        try {
            _state.value = TorrentFileDownloadState.Downloading
            pendingFileId = torrentFileRepository.downloadTorrentFile(url)

            val event = if (pendingFileId != null) {
                TorrentFileDownloadEvent.ReadyToWrite(fileName)
            } else {
                TorrentFileDownloadEvent.FileNotFound
            }
            _events.send(event)
        } catch (e: CancellationException) {
            // Never catch this.
            throw e
        } catch (e: Throwable) {
            _events.send(TorrentFileDownloadEvent.DownloadFailed(e.message))
        } finally {
            _state.value = TorrentFileDownloadState.Idle
        }
    }

    /**
     * Attempts to download a file using the given info hash.
     */
    suspend fun tryDownloadUsingInfoHash(infoHash: String, fileName: String) {
        val url = "https://itorrents.net/torrent/${infoHash.uppercase()}.torrent"
        download(url, fileName)
    }

    /**
     * Writes the content of the pending file to the given stream.
     */
    suspend fun writeFileContent(outputStream: OutputStream) {
        val pendingFileId = pendingFileId ?: return

        try {
            _state.value = TorrentFileDownloadState.Writing
            torrentFileRepository.writeTorrentFile(
                fileId = pendingFileId,
                outputStream = outputStream,
            )

            _events.send(TorrentFileDownloadEvent.WriteSucceed)
        } catch (e: IOException) {
            _events.send(TorrentFileDownloadEvent.WriteFailed(e.message))
        } finally {
            resetState()
        }
    }

    /**
     * Resets state back to the default.
     */
    private fun resetState() {
        _state.value = TorrentFileDownloadState.Idle
        pendingFileId = null
    }
}