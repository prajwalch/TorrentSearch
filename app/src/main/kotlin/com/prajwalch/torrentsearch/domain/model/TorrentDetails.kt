package com.prajwalch.torrentsearch.domain.model

data class TorrentDetails(
    val infoHash: String,
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
    val magnetUri: String,
    val fileDownloadLink: String? = null,
)