package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.network.NetworkClient

import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess

import kotlinx.coroutines.CancellationException

sealed interface TorrentFileDownloadResult {
    data class Success(val content: ByteArray) : TorrentFileDownloadResult

    data object Failed : TorrentFileDownloadResult

    data object FileNotFound : TorrentFileDownloadResult
}

/**
 * Manages and handles torrent file downloading related task.
 */
class TorrentFileDownloader(private val networkClient: NetworkClient) {
    /**
     * An in-memory cache for saving downloaded torrent files.
     */
    private val contentCache = mutableMapOf<String, ByteArray>()

    /**
     * Attempts to download a torrent file using the given info hash.
     */
    suspend fun tryDownloadUsingInfoHash(infoHash: String): TorrentFileDownloadResult {
        val url = "https://itorrents.net/torrent/${infoHash.uppercase()}.torrent"
        return download(url)
    }

    /**
     * Downloads a torrent file from the given URL.
     */
    suspend fun download(url: String): TorrentFileDownloadResult = try {
        val cachedContent = contentCache[url]
        if (cachedContent != null) {
            return TorrentFileDownloadResult.Success(cachedContent)
        }

        val response = networkClient.get(url)
        if (!response.status.isSuccess()) {
            return TorrentFileDownloadResult.FileNotFound
        }

        val content = response.bodyAsBytes()
        contentCache[url] = content

        TorrentFileDownloadResult.Success(content)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        TorrentFileDownloadResult.Failed
    }
}