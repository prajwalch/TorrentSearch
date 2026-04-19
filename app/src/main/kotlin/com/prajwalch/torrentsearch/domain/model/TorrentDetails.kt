package com.prajwalch.torrentsearch.domain.model

data class TorrentDetails(
    val infoHash: String,
    val name: String,
    val size: String? = null,
    val seeders: UInt? = null,
    val peers: UInt? = null,
    val uploadDate: String? = null,
    val category: Category? = null,
    val uploader: String? = null,
    val lastChecked: String? = null,
    val magnetUri: String,
    val fileDownloadLink: String? = null,
    val description: String? = null,
    val posterUrl: String? = null,
    val screenshotUrls: List<String> = emptyList(),
)