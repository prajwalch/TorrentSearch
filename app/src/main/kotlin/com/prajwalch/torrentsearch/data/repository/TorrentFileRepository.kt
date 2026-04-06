package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.network.HttpClient

import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray

import java.io.OutputStream
import java.util.UUID

import javax.inject.Inject

typealias TorrentFileId = UUID

/**
 * A repository for downloading and managing torrent file.
 */
class TorrentFileRepository @Inject constructor(private val httpClient: HttpClient) {
    /**
     * A short-lived cache for saving downloaded torrent files.
     */
    private val contentCache = mutableMapOf<TorrentFileId, ByteArray>()

    /**
     * Downloads the file from the given URL and returns an ID which should be
     * later passed to [writeTorrentFile] for writing content to output stream.
     */
    suspend fun downloadTorrentFile(url: String): TorrentFileId? {
        val id = UUID.nameUUIDFromBytes(url.toByteArray())
        if (contentCache.containsKey(id)) return id

        val fileContent = getTorrentFile(url = url) ?: return null
        contentCache[id] = fileContent

        return id
    }

    /**
     * Attempts to get the file content from the given URL.
     *
     * Returns content as [ByteArray] if remote host successfully returns it
     * otherwise returns `null`.
     */
    private suspend fun getTorrentFile(url: String): ByteArray? = withContext(Dispatchers.IO) {
        val response = httpClient.getResponse(url = url)
        if (!response.status.isSuccess()) return@withContext null

        response.bodyAsChannel().readRemaining().readByteArray()
    }

    /**
     * Writes the content associated with given file ID to given outstream.
     */
    suspend fun writeTorrentFile(fileId: TorrentFileId, outputStream: OutputStream) {
        withContext(Dispatchers.IO) {
            val fileContent = contentCache[fileId]
            fileContent?.let(outputStream::write)
        }
    }
}