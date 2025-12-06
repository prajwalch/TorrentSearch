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
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val filterQuery = MutableStateFlow("")
    private val sortOptions = MutableStateFlow(SortOptions())

    val uiState = combine(
        filterQuery,
        sortOptions,
        bookmarksRepository.observeAllBookmarks(),
        settingsRepository.enableNSFWMode,
    ) { filterQuery, sortOptions, bookmarks, nsfwModeEnabled ->
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

    /** Sorts the current bookmarks. */
    fun sortBookmarks(criteria: SortCriteria, order: SortOrder) {
        sortOptions.value = SortOptions(criteria = criteria, order = order)
    }

    /** Filters the bookmarks using the given query. */
    fun filterBookmarks(query: String) {
        filterQuery.value = query
    }
}