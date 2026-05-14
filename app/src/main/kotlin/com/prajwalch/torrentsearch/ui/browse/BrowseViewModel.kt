package com.prajwalch.torrentsearch.ui.browse

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarkRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.ViewedTorrentRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersGateway
import com.prajwalch.torrentsearch.domain.TorrentFileDownloadEvent
import com.prajwalch.torrentsearch.domain.TorrentFileDownloadState
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.network.ConnectivityChecker

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.io.OutputStream
import javax.inject.Inject

import kotlin.time.Duration.Companion.seconds

data class BrowseUiState(
    val contentState: BrowseContentState = BrowseContentState.Loading,
    val torrents: ImmutableList<Torrent> = persistentListOf(),
    val queryParams: BrowseQueryParams = BrowseQueryParams(),
    val viewFilters: BrowseViewFilters = BrowseViewFilters(),
    val viewedTorrentHashes: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
)

@Stable
sealed interface BrowseContentState {
    data object Loading : BrowseContentState
    data object InternetError : BrowseContentState
    data object Ready : BrowseContentState
}

data class BrowseQueryParams(
    val sort: BrowseSort = BrowseSort.Latest,
    val category: Category = Category.All,
)

data class BrowseViewFilters(
    val deadTorrents: Boolean = true,
    val hideViewed: Boolean = false,
)

enum class BrowseSort {
    Latest,
    Top,
}

/**
 * A ViewModel that handles the business logic of browse screen.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    searchProvidersGateway: SearchProvidersGateway,
    connectivityChecker: ConnectivityChecker,
    settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val viewedTorrentRepository: ViewedTorrentRepository,
    private val torrentFileDownloader: TorrentFileDownloader,
) : ViewModel() {
    /**
     * A torrents' loader.
     */
    private val torrentsLoader = TorrentsLoader(
        scope = viewModelScope,
        searchProvidersGateway = searchProvidersGateway,
        connectivityChecker = connectivityChecker,
    )

    /**
     * A processor for applying different filters.
     */
    private val torrentsProcessor = TorrentsProcessor(
        torrents = torrentsLoader.torrents,
        queryParams = torrentsLoader.queryParams,
        viewedTorrentHashes = viewedTorrentRepository.getAllViewedHashes(),
        settingsRepository = settingsRepository,
    )

    /**
     * The primary, read-only UI state.
     */
    val uiState = combine(
        torrentsLoader.queryParams,
        torrentsLoader.state,
        torrentsProcessor.processedTorrents,
        torrentsProcessor.viewFilters,
        viewedTorrentRepository.getAllViewedHashes(),
    ) {
            queryParams,
            loaderState,
            torrents,
            viewFilters,
            viewedTorrentHashes,
        ->
        val contentState = when {
            loaderState.isLoading -> BrowseContentState.Loading
            loaderState.isInternetError -> BrowseContentState.InternetError
            else -> BrowseContentState.Ready
        }

        BrowseUiState(
            contentState = contentState,
            torrents = torrents,
            queryParams = queryParams,
            viewFilters = viewFilters,
            viewedTorrentHashes = viewedTorrentHashes,
            isSearching = loaderState.isSearching,
            isRefreshing = loaderState.isRefreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = BrowseUiState(),
    )

    /**
     * The globally observable, read-only state of the torrent file downloader.
     */
    val torrentFileDownloadState: StateFlow<TorrentFileDownloadState> =
        torrentFileDownloader.state

    /**
     * The globally observable, read-only events of the torrent file downloader.
     */
    val torrentFileDownloadEvents: Flow<TorrentFileDownloadEvent> =
        torrentFileDownloader.events

    init {
        loadTorrents()
    }

    fun loadTorrents() {
        torrentsLoader.load()
    }

    fun refreshTorrents() {
        torrentsLoader.refresh()
    }

    fun updateBrowseSort(sort: BrowseSort) {
        torrentsLoader.updateBrowseSort(sort)
    }

    fun updateCategory(category: Category) {
        torrentsLoader.updateCategory(category)
    }

    fun toggleDeadTorrents() {
        torrentsProcessor.toggleDeadTorrents()
    }

    fun toggleHideViewed() {
        torrentsProcessor.toggleHideViewed()
    }

    fun bookmarkTorrent(torrent: Torrent) {
        viewModelScope.launch {
            bookmarkRepository.bookmarkTorrent(torrent = torrent)
        }
    }

    fun markAsViewed(infoHash: String) {
        viewModelScope.launch {
            viewedTorrentRepository.markAsViewed(infoHash)
        }
    }

    fun downloadTorrentFile(url: String, fileName: String) {
        viewModelScope.launch {
            torrentFileDownloader.download(url = url, fileName = fileName)
        }
    }

    fun downloadTorrentFileUsingInfoHash(infoHash: String, fileName: String) {
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

/**
 * Manages and coordinates the torrents loading task.
 *
 * It is the first stage in the load pipeline which handles the task of fetching
 * torrents, and managing load related states.
 */
private class TorrentsLoader(
    /**
     * The [CoroutineScope] in which the load is performed.
     */
    private val scope: CoroutineScope,
    /**
     * A gateway for interacting with different providers.
     */
    private val searchProvidersGateway: SearchProvidersGateway,
    /**
     * A helper class for checking network condition.
     */
    private val connectivityChecker: ConnectivityChecker,
) {
    /**
     * Represents the state of loader.
     */
    data class State(
        val isLoading: Boolean = false,
        val isSearching: Boolean = false,
        val isRefreshing: Boolean = false,
        val isInternetError: Boolean = false,
    )

    /**
     * The internal, mutable source of truth for the query params.
     *
     * The query params are updated only on-demand and when doing so, a new
     * load job is automatically started to fetch new torrents based on
     * updated query params.
     */
    private val _queryParams = MutableStateFlow(BrowseQueryParams())

    /**
     * The publicly observable, read-only state of the query params.
     */
    val queryParams = _queryParams.asStateFlow()

    /**
     * The internal, mutable source of truth for the loader state.
     * This flow is updated constantly during the load lifecycle.
     */
    private val _state = MutableStateFlow(State(isLoading = true))

    /**
     * The publicly observable, read-only state of the loader.
     */
    val state = _state.asStateFlow()

    /**
     * The internal, mutable source of truth for the torrents.
     */
    private val _torrents = MutableStateFlow(emptyList<Torrent>())

    /**
     * The publicly observable, read-only state of raw and unprocessed
     * torrents.
     *
     * Downstream pipelines should use this flow for further processing.
     */
    val torrents = _torrents.asStateFlow()

    /**
     * An ongoing background load job.
     *
     * Before starting a new lifecycle the ongoing job is explicitly canceled
     * whether it has completed or not to prevent unnecessary usage of resource.
     */
    private var loadJob: Job? = null

    /**
     * Replaces the current sort with [sort] and triggers a fresh torrents fetch.
     */
    fun updateBrowseSort(sort: BrowseSort) {
        _queryParams.update { it.copy(sort = sort) }
        load()
    }

    /**
     * Replaces the current category with [category] and triggers a fresh
     * torrents fetch.
     */
    fun updateCategory(category: Category) {
        _queryParams.update { it.copy(category = category) }
        load()
    }

    /**
     * Initiates a new torrents fetch.
     */
    fun load() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _state.update { it.copy(isLoading = true, isInternetError = false) }

            if (!connectivityChecker.isInternetAvailable()) {
                _state.update { it.copy(isLoading = false, isInternetError = true) }
                return@launch
            }

            loadTorrents()
        }
    }

    /**
     * Refreshes current torrents by triggering a new fetch.
     */
    fun refresh() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _state.update { it.copy(isRefreshing = true) }

            if (!connectivityChecker.isInternetAvailable()) {
                _state.update { it.copy(isRefreshing = false) }
                return@launch
            }

            loadTorrents()
        }
    }

    /**
     * Loads new torrents based on the current query params.
     */
    private suspend fun loadTorrents() {
        val queryParams = _queryParams.value
        val torrentsFlow = when (queryParams.sort) {
            BrowseSort.Latest -> searchProvidersGateway.getLatestTorrents(queryParams.category)
            BrowseSort.Top -> searchProvidersGateway.getTopTorrents(queryParams.category)
        }

        torrentsFlow
            .onStart {
                _state.update { it.copy(isSearching = true) }
            }
            .onCompletion {
                _state.update { it.copy(isSearching = false) }
            }
            .collect { torrents ->
                _torrents.value = torrents
                _state.update { it.copy(isLoading = false, isRefreshing = false) }
            }
    }
}

/**
 * Setups and manages the execution of different *intermediate* transformation
 * operations on the [torrents].
 *
 * It is the second stage in the load pipeline which applies view filters and
 * sorting operations.
 */
private class TorrentsProcessor(
    /**
     * The input stream from where torrents are pulled.
     */
    torrents: Flow<List<Torrent>>,
    /**
     * Query params that is used to fetch torrents.
     *
     * Query params are used additionally to remove any unwanted torrents.
     */
    queryParams: Flow<BrowseQueryParams>,
    /**
     * Flow of viewed torrent hashes for filtering.
     */
    viewedTorrentHashes: Flow<Set<String>>,
    /**
     * The repository for fetching user-defined transformation options.
     */
    settingsRepository: SettingsRepository,
) {
    /**
     * The internal, mutable state of the view filters.
     */
    private val _viewFilters = MutableStateFlow(BrowseViewFilters())

    /**
     * The publicly observable, read-only state of the view filters.
     */
    val viewFilters = _viewFilters.asStateFlow()

    /**
     * Hashes of currently viewed torrents that should be hidden when filter is active.
     *
     * The hashes are captured only when 'hide viewed' filter is enabled to avoid
     * instant hiding.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentlyViewedTorrentHashes: Flow<Set<String>> =
        _viewFilters
            .map { it.hideViewed }
            .map { if (it) viewedTorrentHashes.firstOrNull().orEmpty() else emptySet() }

    /**
     * The output stream of processed torrents.
     */
    val processedTorrents: Flow<ImmutableList<Torrent>> =
        combine(
            torrents,
            queryParams,
            _viewFilters,
            currentlyViewedTorrentHashes,
            settingsRepository.enableNSFWMode,
            ::processTorrents,
        ).flowOn(Dispatchers.Default)

    /**
     * Processes and returns a new list containing processed torrents based
     * on the given filters and other options.
     */
    private fun processTorrents(
        torrents: List<Torrent>,
        queryParams: BrowseQueryParams,
        viewFilters: BrowseViewFilters,
        viewedTorrentHashes: Set<String>,
        nsfwModeEnabled: Boolean,
    ): ImmutableList<Torrent> {
        val sortComparator: Comparator<Torrent> = when (queryParams.sort) {
            BrowseSort.Latest -> compareByDescending { torrent -> torrent.uploadDate }
            BrowseSort.Top -> compareByDescending { torrent -> torrent.seeders }
        }
        val predicates: List<(Torrent) -> Boolean> = buildList {
            if (!nsfwModeEnabled) add { !it.isNSFW }
            if (!viewFilters.deadTorrents) add { !it.isDead }
            if (viewFilters.hideViewed) add { it.infoHash !in viewedTorrentHashes }
            if (queryParams.category != Category.All) add { queryParams.category == it.category }
        }
        val filteredTorrents = if (predicates.isEmpty()) {
            torrents
        } else {
            torrents.filter { torrent -> predicates.all { it(torrent) } }
        }

        return filteredTorrents.sortedWith(comparator = sortComparator).toImmutableList()
    }

    /**
     * Shows or hides dead torrents.
     */
    fun toggleDeadTorrents() {
        _viewFilters.update { it.copy(deadTorrents = !it.deadTorrents) }
    }

    /**
     * Toggles the "hide viewed" filter.
     */
    fun toggleHideViewed() {
        _viewFilters.update { it.copy(hideViewed = !it.hideViewed) }
    }
}