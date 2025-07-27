package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prajwalch.torrentsearch.data.SettingsRepository

import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.domain.FilterNSFWTorrentsUseCase
import com.prajwalch.torrentsearch.domain.SortCriteria
import com.prajwalch.torrentsearch.domain.SortOrder
import com.prajwalch.torrentsearch.domain.SortTorrentsUseCase
import com.prajwalch.torrentsearch.models.Torrent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the Bookmarks screen. */
data class BookmarksUiState(
    val bookmarks: List<Torrent> = emptyList(),
    val currentSortCriteria: SortCriteria = SortCriteria.Default,
    val currentSortOrder: SortOrder = SortOrder.Default,
)

/** ViewModel that handles the business logic of Bookmarks screen. */
class BookmarksViewModel(
    private val settingsRepository: SettingsRepository,
    private val torrentsRepository: TorrentsRepository,
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
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /** Current bookmarks acquired from the repository. */
    private val bookmarks: StateFlow<List<Torrent>> = torrentsRepository
        .bookmarkedTorrents
        .map { bookmarks ->
            SortTorrentsUseCase(
                torrents = bookmarks,
                criteria = _uiState.value.currentSortCriteria,
                order = _uiState.value.currentSortOrder,
            )()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
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
            val nsfwFilteredBookmarks = filterNSFWBookmarks(
                bookmarks = bookmarks,
                isNSFWModeEnabled = enableNSFWMode.value,
            )
            _uiState.update { it.copy(bookmarks = nsfwFilteredBookmarks) }
        }
    }

    /** Observes the NSFW settings and update the UI accordingly. */
    private fun observeNSFWModeSetting() = viewModelScope.launch {
        enableNSFWMode.collect { enableNSFWMode ->
            val nsfwFilteredBookmarks = filterNSFWBookmarks(
                bookmarks = bookmarks.value,
                isNSFWModeEnabled = enableNSFWMode,
            )

            _uiState.update { it.copy(bookmarks = nsfwFilteredBookmarks) }
        }
    }

    /** Filters the NSFW bookmarks and returns a new list. */
    private fun filterNSFWBookmarks(
        bookmarks: List<Torrent>,
        isNSFWModeEnabled: Boolean,
    ): List<Torrent> = if (isNSFWModeEnabled) {
        bookmarks
    } else {
        FilterNSFWTorrentsUseCase(torrents = bookmarks)()
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

    /** Sorts the current bookmarks. */
    fun sortBookmarks(criteria: SortCriteria, order: SortOrder) {
        val sortedBookmarks = SortTorrentsUseCase(
            torrents = _uiState.value.bookmarks,
            criteria = criteria,
            order = order,
        )()

        _uiState.update {
            it.copy(
                bookmarks = sortedBookmarks,
                currentSortCriteria = criteria,
                currentSortOrder = order,
            )
        }
    }

    companion object {
        /** Provides a factory function for [BookmarksViewModel]. */
        @Suppress("UNCHECKED_CAST")
        fun provideFactory(
            settingsRepository: SettingsRepository,
            torrentsRepository: TorrentsRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookmarksViewModel(
                        settingsRepository = settingsRepository,
                        torrentsRepository = torrentsRepository,
                    ) as T
                }
            }
        }
    }
}