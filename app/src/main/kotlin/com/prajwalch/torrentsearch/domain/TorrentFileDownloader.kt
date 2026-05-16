package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.network.HttpClient
import dagger.hilt.android.scopes.ViewModelScoped

import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray

import java.io.IOException
import java.io.OutputStream
import java.util.UUID

import javax.inject.Inject

private typealias TorrentFileId = UUID

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
class TorrentFileDownloader @Inject constructor(private val httpClient: HttpClient) {
    /**
     * An in-memory cache for saving downloaded torrent files.
     */
    private val contentCache = mutableMapOf<TorrentFileId, ByteArray>()

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
     * Attempts to download a torrent file using the given info hash.
     */
    suspend fun tryDownloadUsingInfoHash(infoHash: String, fileName: String) {
        val url = "https://itorrents.net/torrent/${infoHash.uppercase()}.torrent"
        download(url, fileName)
    }

    /**
     * Downloads a torrent file from the given URL.
     */
    suspend fun download(url: String, fileName: String): Unit = try {
        resetState()

        _state.value = TorrentFileDownloadState.Downloading
        pendingFileId = downloadFile(url)

        val event = if (pendingFileId != null) {
            val normalizedFileName = fileName.replace(' ', '-')
            TorrentFileDownloadEvent.ReadyToWrite(normalizedFileName)
        } else {
            TorrentFileDownloadEvent.FileNotFound
        }
        _events.send(event)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        _events.send(TorrentFileDownloadEvent.DownloadFailed(e.message))
    } finally {
        _state.value = TorrentFileDownloadState.Idle
    }

    private suspend fun downloadFile(url: String): TorrentFileId? = withContext(Dispatchers.IO) {
        val id = UUID.nameUUIDFromBytes(url.toByteArray())
        if (contentCache.containsKey(id)) return@withContext id

        val response = httpClient.getResponse(url = url)
        if (!response.status.isSuccess()) return@withContext null

        val fileContent = response.bodyAsChannel().readRemaining().readByteArray()
        contentCache[id] = fileContent

        id
    }

    /**
     * Writes the content of the pending file to the given stream.
     */
    suspend fun writeFileContent(outputStream: OutputStream): Unit = withContext(Dispatchers.IO) {
        val pendingFileId = pendingFileId ?: return@withContext

        try {
            _state.value = TorrentFileDownloadState.Writing

            val fileContent = contentCache[pendingFileId]
            fileContent?.let(outputStream::write)

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