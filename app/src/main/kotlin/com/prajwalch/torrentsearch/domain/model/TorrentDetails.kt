package com.prajwalch.torrentsearch.domain.model

import com.prajwalch.torrentsearch.util.TorrentUtils

data class TorrentDetails(
    val name: String,
    val size: String,
    val seeders: UInt,
    val peers: UInt,
    val uploadDate: String,
    val category: String? = null,
    val uploader: String? = null,
    val lastChecked: String? = null,
    val description: String? = null,
    val posterUrl: String? = null,
    val screenshotUrls: List<String> = emptyList(),
    val infoHashOrMagnetUri: InfoHashOrMagnetUri,
    val fileDownloadLink: String? = null,
) {
    fun infoHash(): String = when (infoHashOrMagnetUri) {
        is InfoHashOrMagnetUri.InfoHash -> infoHashOrMagnetUri.hash
        is InfoHashOrMagnetUri.MagnetUri -> {
            TorrentUtils.getInfoHashFromMagnetUri(infoHashOrMagnetUri.uri)
        }
    }
}