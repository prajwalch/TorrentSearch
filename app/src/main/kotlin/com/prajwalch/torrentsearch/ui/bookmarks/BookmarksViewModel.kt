package com.prajwalch.torrentsearch.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarksRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.extensions.customSort
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOptions
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val bookmarksRepository: BookmarksRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val filterQuery = MutableStateFlow("")

    val uiState = combine(
        filterQuery,
        bookmarksRepository.observeAllBookmarks(),
        settingsRepository.enableNSFWMode,
        settingsRepository.bookmarksSortOptions,
    ) { filterQuery, bookmarks, nsfwModeEnabled, sortOptions ->
        val bookmarks = bookmarks
            .filter { nsfwModeEnabled || !it.isNSFW() }
            .filter { filterQuery.isBlank() || it.name.contains(filterQuery, ignoreCase = true) }
            .customSort(criteria = sortOptions.criteria, order = sortOptions.order)

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
            bookmarksRepository.deleteBookmarkedTorrent(torrent)
        }
    }

    /** Deletes all bookmarks. */
    fun deleteAllBookmarks() {
        viewModelScope.launch {
            bookmarksRepository.deleteAllBookmarks()
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
}