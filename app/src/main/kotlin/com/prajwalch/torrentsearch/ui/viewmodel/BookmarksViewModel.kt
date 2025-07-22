package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prajwalch.torrentsearch.data.BookmarksRepository
import com.prajwalch.torrentsearch.models.Torrent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(private val repository: BookmarksRepository) : ViewModel() {
    val bookmarks: StateFlow<List<Torrent>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun add(torrent: Torrent) {
        viewModelScope.launch {
            repository.add(torrent)
        }
    }

    fun delete(torrent: Torrent) {
        viewModelScope.launch {
            repository.delete(torrent)
        }
    }

    companion object {
        /** Provides a factory function for [BookmarksViewModel]. */
        @Suppress("UNCHECKED_CAST")
        fun provideFactory(bookmarksRepository: BookmarksRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookmarksViewModel(bookmarksRepository) as T
                }
            }
        }
    }
}