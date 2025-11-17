package com.prajwalch.torrentsearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarksRepository
import com.prajwalch.torrentsearch.models.Torrent

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentSearchAppViewModel @Inject constructor(
    private val bookmarksRepository: BookmarksRepository,
) : ViewModel() {
    fun bookmarkTorrent(torrent: Torrent) {
        viewModelScope.launch {
            bookmarksRepository.bookmarkTorrent(torrent = torrent)
        }
    }

    fun deleteBookmarkedTorrent(torrent: Torrent) {
        viewModelScope.launch {
            bookmarksRepository.deleteBookmarkedTorrent(torrent = torrent)
        }
    }
}