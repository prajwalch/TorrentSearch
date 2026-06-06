package com.prajwalch.torrentsearch.ui.torrentdetails

import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersGateway
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

import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

data class TorrentDetailsUiState(
    val state: TorrentDetailsState = TorrentDetailsState.Loading,
    val isRefreshing: Boolean = false,
    val blurNSFWImages: Boolean = true,
)

@Stable
sealed interface TorrentDetailsState {
    data object Loading : TorrentDetailsState
    data object NoInternetConnection : TorrentDetailsState
    data object Unavailable : TorrentDetailsState
    data class UnsupportedTorrentSite(val host: String) : TorrentDetailsState
    data class SomethingWentWrong(val message: String?) : TorrentDetailsState
    data class Available(val details: TorrentDetails) : TorrentDetailsState
}

@HiltViewModel
class TorrentDetailsViewModel @Inject constructor(
    private val searchProvidersGateway: SearchProvidersGateway,
    private val torrentFileDownloader: TorrentFileDownloader,
    private val connectivityChecker: ConnectivityChecker,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val detailsPageUrl: String = savedStateHandle["detailsPageUrl"]
        ?: error("TorrentDetailsViewModel can't function without details page URL")

    val providerName: String = savedStateHandle["providerName"]
        ?: error("TorrentDetailsViewModel can't function without provider name")

    private val detailsState: MutableStateFlow<TorrentDetailsState> =
        MutableStateFlow(TorrentDetailsState.Loading)
    private val isRefreshing = MutableStateFlow(false)

    val uiState = combine(
        detailsState,
        isRefreshing,
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
            detailsState.value = TorrentDetailsState.Loading
            detailsState.value = getTorrentDetails()
        }
    }

    fun refreshDetails() {
        isRefreshing.value = true
        viewModelScope.launch {
            val details = getTorrentDetails()
            if (details is TorrentDetailsState.Available) {
                detailsState.value = details
            }

            isRefreshing.value = false
        }
    }

    private suspend fun getTorrentDetails(): TorrentDetailsState = try {
        val response = searchProvidersGateway.getTorrentDetails(
            detailsPageUrl = detailsPageUrl,
            providerName = providerName,
        )

        when (response) {
            GetTorrentDetailsResponse.Unavailable -> {
                TorrentDetailsState.Unavailable
            }

            GetTorrentDetailsResponse.UnsupportedUrl -> {
                val uri = detailsPageUrl.toUri()
                TorrentDetailsState.UnsupportedTorrentSite(host = uri.host!!)
            }

            is GetTorrentDetailsResponse.Success -> {
                TorrentDetailsState.Available(response.details)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        if (!connectivityChecker.isInternetAvailable()) {
            TorrentDetailsState.NoInternetConnection
        } else {
            TorrentDetailsState.SomethingWentWrong(e.message)
        }
    } catch (e: Throwable) {
        TorrentDetailsState.SomethingWentWrong(e.message)
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