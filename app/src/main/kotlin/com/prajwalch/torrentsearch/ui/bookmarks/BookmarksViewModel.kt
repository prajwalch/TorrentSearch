package com.prajwalch.torrentsearch.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorrentsRepository
import com.prajwalch.torrentsearch.extensions.customSort
import com.prajwalch.torrentsearch.extensions.filterNSFW
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder
import com.prajwalch.torrentsearch.models.Torrent

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

/** UI state for the Bookmarks screen. */
data class BookmarksUiState(
    val bookmarks: List<Torrent> = emptyList(),
    val currentSortCriteria: SortCriteria = SortCriteria.Default,
    val currentSortOrder: SortOrder = SortOrder.Default,
)

/** ViewModel that handles the business logic of Bookmarks screen. */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val torrentsRepository: TorrentsRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    /**
     * Current UI state.
     *
     * Automatically updated when new bookmarks are added.
     */
    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState = _uiState.asStateFlow()

    /** Current NSFW mode setting. */
    private val enableNSFWMode: StateFlow<Boolean> = settingsRepository
        .enableNSFWMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    /** Current bookmarks acquired from the repository. */
    private val bookmarks: StateFlow<List<Torrent>> = torrentsRepository
        .bookmarkedTorrents
        .map { bookmarks ->
            bookmarks.customSort(
                criteria = _uiState.value.currentSortCriteria,
                order = _uiState.value.currentSortOrder,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        observeBookmarks()
        observeNSFWModeSetting()
    }

    /**
     * Observes the new bookmarks and the automatically updates the UI state
     * after processing them.
     */
    private fun observeBookmarks() = viewModelScope.launch {
        bookmarks.collect { bookmarks ->
            val nsfwFilteredBookmarks = bookmarks.filterNSFW(
                isNSFWModeEnabled = enableNSFWMode.value,
            )
            _uiState.update { it.copy(bookmarks = nsfwFilteredBookmarks) }
        }
    }

    /** Observes the NSFW settings and update the UI accordingly. */
    private fun observeNSFWModeSetting() = viewModelScope.launch {
        enableNSFWMode.collect { enableNSFWMode ->
            val nsfwFilteredBookmarks = bookmarks.value.filterNSFW(
                isNSFWModeEnabled = enableNSFWMode,
            )

            _uiState.update { it.copy(bookmarks = nsfwFilteredBookmarks) }
        }
    }

    /**
     * Bookmarks the given torrent.
     *
     * TODO: This function is not used directly from the Bookmarks screen
     *       instead it is being used by the root screen.
     *
     */
    fun bookmarkTorrent(torrent: Torrent) {
        viewModelScope.launch {
            torrentsRepository.bookmarkTorrent(torrent)
        }
    }

    /**
     * Deletes the given bookmarked torrent.
     *
     * TODO: This function is not used directly from the Bookmarks screen
     *       instead it is being used by the root screen.
     *
     */
    fun deleteBookmarkedTorrent(torrent: Torrent) {
        viewModelScope.launch {
            torrentsRepository.deleteBookmarkedTorrent(torrent)
        }
    }

    /** Deletes all bookmarks. */
    fun deleteAllBookmarks() {
        viewModelScope.launch {
            torrentsRepository.deleteAllBookmarks()
        }
    }

    /** Sorts the current bookmarks. */
    fun sortBookmarks(criteria: SortCriteria, order: SortOrder) {
        val sortedBookmarks = _uiState.value.bookmarks.customSort(
            criteria = criteria,
            order = order,
        )

        _uiState.update {
            it.copy(
                bookmarks = sortedBookmarks,
                currentSortCriteria = criteria,
                currentSortOrder = order,
            )
        }
    }
}