package com.prajwalch.torrentsearch.ui.torrentdetails

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.ConnectivityChecker

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import java.io.OutputStream
import javax.inject.Inject

@Stable
sealed interface TorrentDetailsUiState {
    data object Loading : TorrentDetailsUiState

    data object NoInternetConnection : TorrentDetailsUiState

    data object DetailsNotFound : TorrentDetailsUiState

    data object ProviderNotSupported : TorrentDetailsUiState

    data class SomethingWentWrong(val message: String?) : TorrentDetailsUiState

    data class LoadSucceed(val details: TorrentDetails) : TorrentDetailsUiState
}

@HiltViewModel
class TorrentDetailsViewModel @Inject constructor(
    searchProvidersManager: SearchProvidersManager,
    private val torrentFileDownloader: TorrentFileDownloader,
    private val connectivityChecker: ConnectivityChecker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val detailsPageUrl: String = savedStateHandle["detailsPageUrl"]
        ?: error("TorrentDetailsViewModel can't function without details page URL")

    val providerName: String = savedStateHandle["providerName"]
        ?: error("TorrentDetailsViewModel can't function without provider name")

    private val provider = searchProvidersManager.findProviderByName(providerName)

    private val _uiState = MutableStateFlow<TorrentDetailsUiState>(TorrentDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val torrentFileDownloadState = torrentFileDownloader.state
    val torrentFileDownloadEvents = torrentFileDownloader.events

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = TorrentDetailsUiState.Loading

            if (!connectivityChecker.isInternetAvailable()) {
                _uiState.value = TorrentDetailsUiState.NoInternetConnection
                return@launch
            }

            if (provider == null) {
                _uiState.value = TorrentDetailsUiState.ProviderNotSupported
                return@launch
            }

            _uiState.value = try {
                when (val response = provider.getDetails(detailsPageUrl)) {
                    GetTorrentDetailsResponse.DetailsNotFound -> {
                        TorrentDetailsUiState.DetailsNotFound
                    }

                    GetTorrentDetailsResponse.RequestNotSupported -> {
                        TorrentDetailsUiState.ProviderNotSupported
                    }

                    is GetTorrentDetailsResponse.Success -> {
                        TorrentDetailsUiState.LoadSucceed(response.details)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                TorrentDetailsUiState.SomethingWentWrong(e.message)
            }
        }
    }

    fun downloadTorrentFile(url: String, fileName: String) {
        viewModelScope.launch {
            torrentFileDownloader.download(url = url, fileName = fileName)
        }
    }

    fun downloadTorrentFileFromInfoHash(infoHash: String, fileName: String) {
        viewModelScope.launch {
            torrentFileDownloader.tryDownloadUsingInfoHash(
                infoHash = infoHash,
                fileName = fileName,
            )
        }
    }

    fun writeTorrentFile(outputStream: OutputStream) {
        viewModelScope.launch {
            torrentFileDownloader.writeFileContent(outputStream)
        }
    }
}