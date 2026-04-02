package com.prajwalch.torrentsearch.ui.torrentdetails

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.torrentfiledownloader.TorrentFileDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

@Stable
sealed interface TorrentDetailsUiState {
    data object Loading : TorrentDetailsUiState

    data class LoadFailed(val error: LoadError) : TorrentDetailsUiState

    data class Ready(val details: TorrentDetails) : TorrentDetailsUiState
}

enum class LoadError {
    UnsupportedSearchProvider,
    DetailsNotFound,
}

@HiltViewModel
class TorrentDetailsViewModel @Inject constructor(
    private val searchProvidersManager: SearchProvidersManager,
    private val torrentFileDownloader: TorrentFileDownloader,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val detailsPageUrl: String = savedStateHandle["detailsPageUrl"]
        ?: error("TorrentDetailsViewModel can't function without details page URL")

    private val providerName: String = savedStateHandle["providerName"]
        ?: error("TorrentDetailsViewModel can't function without provider name")

    private val _uiState = MutableStateFlow<TorrentDetailsUiState>(TorrentDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val torrentFileDownloadState = torrentFileDownloader.state
    val torrentFileDownloadEvents = torrentFileDownloader.events

    init {
        loadDetails()
    }

    private fun loadDetails() = viewModelScope.launch {
        val searchProvider = searchProvidersManager.findSearchProviderByName(providerName)
        if (searchProvider == null) {
            _uiState.value = TorrentDetailsUiState.LoadFailed(LoadError.UnsupportedSearchProvider)
            return@launch
        }

        val details = searchProvider.getDetails(detailsPageUrl)
        if (details == null) {
            _uiState.value = TorrentDetailsUiState.LoadFailed(LoadError.DetailsNotFound)
        } else {
            _uiState.value = TorrentDetailsUiState.Ready(details)
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
            torrentFileDownloader.writeFile(outputStream = outputStream)
        }
    }
}