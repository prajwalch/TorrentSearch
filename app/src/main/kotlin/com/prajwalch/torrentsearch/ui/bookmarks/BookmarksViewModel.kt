package com.prajwalch.torrentsearch.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarkRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.torrentfiledownloader.TorrentFileDownloader
import com.prajwalch.torrentsearch.util.createSortComparator

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

import kotlin.time.Duration.Companion.seconds

/** UI state for the Bookmarks screen. */
data class BookmarksUiState(
    val bookmarks: List<Torrent> = emptyList(),
    val sortOptions: SortOptions = SortOptions(),
)

/** ViewModel that handles the business logic of Bookmarks screen. */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val settingsRepository: SettingsRepository,
    private val torrentFileDownloader: TorrentFileDownloader,
) : ViewModel() {
    private val filterQuery = MutableStateFlow("")

    val torrentFileDownloadState = torrentFileDownloader.state
    val torrentFileDownloadEvents = torrentFileDownloader.events
    val uiState = combine(
        filterQuery,
        bookmarkRepository.getAllBookmarks(),
        settingsRepository.enableNSFWMode,
        settingsRepository.bookmarksSortOptions,
    ) { filterQuery, bookmarks, nsfwModeEnabled, sortOptions ->
        val bookmarks = bookmarks
            .filter { nsfwModeEnabled || !it.isNSFW }
            .filter { filterQuery.isBlank() || it.name.contains(filterQuery, ignoreCase = true) }
            .sortedWith(
                createSortComparator(criteria = sortOptions.criteria, order = sortOptions.order)
            )

        BookmarksUiState(
            bookmarks = bookmarks,
            sortOptions = sortOptions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = BookmarksUiState(),
    )

    /** Deletes the given bookmarked torrent. */
    fun deleteBookmarkedTorrent(torrent: Torrent) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmarkedTorrent(torrent)
        }
    }

    /** Deletes all bookmarks. */
    fun deleteAllBookmarks() {
        viewModelScope.launch {
            bookmarkRepository.deleteAllBookmarks()
        }
    }

    /** Sets or updates the sort criteria. */
    fun setSortCriteria(criteria: SortCriteria) {
        viewModelScope.launch {
            settingsRepository.setBookmarksSortCriteria(criteria)
        }
    }

    /** Sets or updates the sort order. */
    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            settingsRepository.setBookmarksSortOrder(order)
        }
    }

    /** Filters the bookmarks using the given query. */
    fun filterBookmarks(query: String) {
        filterQuery.value = query
    }

    /** Attempts to import bookmarks from the given stream. */
    fun importBookmarks(inputStream: InputStream) {
        viewModelScope.launch {
            bookmarkRepository.importBookmarks(inputStream = inputStream)
        }
    }

    /** Attempts to export bookmarks to the given stream. */
    fun exportBookmarks(outputStream: OutputStream) {
        viewModelScope.launch {
            bookmarkRepository.exportBookmarks(outputStream = outputStream)
        }
    }

    fun downloadTorrentFile(url: String, fileName: String) {
        viewModelScope.launch {
            torrentFileDownloader.downloadFile(url = url, fileName = fileName)
        }
    }

    fun downloadTorrentFileFromInfoHash(infoHash: String, fileName: String) {
        viewModelScope.launch {
            torrentFileDownloader.downloadFileFromInfoHash(
                infoHash = infoHash,
                fileName = fileName,
            )
        }
    }

    fun writeTorrentFile(outputStream: OutputStream) {
        viewModelScope.launch {
            torrentFileDownloader.writeFile(outputStream)
        }
    }
}