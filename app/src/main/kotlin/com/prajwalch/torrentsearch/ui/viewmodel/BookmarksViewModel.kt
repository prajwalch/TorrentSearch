package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.TorrentsRepository
import com.prajwalch.torrentsearch.models.Torrent

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(private val torrentsRepository: TorrentsRepository) : ViewModel() {
    val bookmarks: StateFlow<List<Torrent>> = torrentsRepository
        .bookmarkedTorrents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun add(torrent: Torrent) {
        viewModelScope.launch {
            torrentsRepository.bookmarkTorrent(torrent)
        }
    }

    fun delete(torrent: Torrent) {
        viewModelScope.launch {
            torrentsRepository.deleteBookmarkedTorrent(torrent)
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