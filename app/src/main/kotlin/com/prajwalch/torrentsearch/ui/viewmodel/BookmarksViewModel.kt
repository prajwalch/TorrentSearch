package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.domain.SortCriteria
import com.prajwalch.torrentsearch.domain.SortOrder
import com.prajwalch.torrentsearch.domain.SortTorrentsUseCase
import com.prajwalch.torrentsearch.models.Torrent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the Bookmarks screen. */
data class BookmarksUiState(
    val bookmarks: List<Torrent> = emptyList(),
    val currentSortCriteria: SortCriteria = SortCriteria.DEFAULT,
    val currentSortOrder: SortOrder = SortOrder.DEFAULT,
)

/** ViewModel that handles the business logic of Bookmarks screen. */
class BookmarksViewModel(private val torrentsRepository: TorrentsRepository) : ViewModel() {
    /**
     * Current UI state.
     *
     * Automatically updated when new bookmarks are added.
     */
    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState = _uiState.asStateFlow()

    /** Current bookmarks acquired from the repository. */
    private val bookmarks: StateFlow<List<Torrent>> = torrentsRepository
        .bookmarkedTorrents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        observeBookmarks()
    }

    /**
     * Observes the new bookmarks and the automatically updates the UI state
     * after processing them.
     */
    private fun observeBookmarks() = viewModelScope.launch {
        bookmarks.collect { bookmarks ->
            val sortedBookmarks = SortTorrentsUseCase(
                torrents = bookmarks,
                criteria = _uiState.value.currentSortCriteria,
                order = _uiState.value.currentSortOrder,
            )()

            _uiState.update { it.copy(bookmarks = sortedBookmarks) }
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
        fun provideFactory(torrentsRepository: TorrentsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookmarksViewModel(torrentsRepository) as T
                }
            }
        }
    }
}