package com.prajwalch.torrentsearch.ui.torrentdetails

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader
import com.prajwalch.torrentsearch.domain.model.GetTorrentDetailsResponse
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.ConnectivityChecker

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.io.OutputStream
import javax.inject.Inject


data class TorrentDetailsUiState(
    val contentState: TorrentDetailsContentState = TorrentDetailsContentState.Loading,
    val blurNSFWImages: Boolean = true,
)

@Stable
sealed interface TorrentDetailsContentState {
    data object Loading : TorrentDetailsContentState

    data object NoInternetConnection : TorrentDetailsContentState

    data object DetailsNotFound : TorrentDetailsContentState

    data object ProviderNotSupported : TorrentDetailsContentState

    data class SomethingWentWrong(val message: String?) : TorrentDetailsContentState

    data class LoadSucceed(val details: TorrentDetails) : TorrentDetailsContentState
}

@HiltViewModel
class TorrentDetailsViewModel @Inject constructor(
    searchProvidersManager: SearchProvidersManager,
    private val torrentFileDownloader: TorrentFileDownloader,
    private val connectivityChecker: ConnectivityChecker,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val detailsPageUrl: String = savedStateHandle["detailsPageUrl"]
        ?: error("TorrentDetailsViewModel can't function without details page URL")

    val providerName: String = savedStateHandle["providerName"]
        ?: error("TorrentDetailsViewModel can't function without provider name")

    private val provider = searchProvidersManager.findProviderByName(providerName)

    private val contentState: MutableStateFlow<TorrentDetailsContentState> =
        MutableStateFlow(TorrentDetailsContentState.Loading)

    val uiState = combine(
        contentState,
        settingsRepository.blurNSFWImages,
        ::TorrentDetailsUiState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TorrentDetailsUiState(),
    )

    val torrentFileDownloadState = torrentFileDownloader.state
    val torrentFileDownloadEvents = torrentFileDownloader.events

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            contentState.value = TorrentDetailsContentState.Loading

            if (!connectivityChecker.isInternetAvailable()) {
                contentState.value = TorrentDetailsContentState.NoInternetConnection
                return@launch
            }

            if (provider == null) {
                contentState.value = TorrentDetailsContentState.ProviderNotSupported
                return@launch
            }

            contentState.value = try {
                when (val response = provider.getDetails(detailsPageUrl)) {
                    GetTorrentDetailsResponse.DetailsNotFound -> {
                        TorrentDetailsContentState.DetailsNotFound
                    }

                    GetTorrentDetailsResponse.RequestNotSupported -> {
                        TorrentDetailsContentState.ProviderNotSupported
                    }

                    is GetTorrentDetailsResponse.Success -> {
                        TorrentDetailsContentState.LoadSucceed(response.details)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                TorrentDetailsContentState.SomethingWentWrong(e.message)
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