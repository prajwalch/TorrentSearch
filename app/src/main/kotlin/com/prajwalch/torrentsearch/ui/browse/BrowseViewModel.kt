package com.prajwalch.torrentsearch.ui.browse

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
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
import com.prajwalch.torrentsearch.domain.model.filter
import com.prajwalch.torrentsearch.domain.model.sortedWithComparator
import com.prajwalch.torrentsearch.network.ConnectivityChecker

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
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
)

@Stable
sealed interface BrowseContentState {
    data object Loading : BrowseContentState
    data object InternetError : BrowseContentState
    data object NotAvailable : BrowseContentState

    sealed interface Available : BrowseContentState {
        data object Complete : Available
        data object Searching : Available
        data object Refreshing : Available
    }
}

data class BrowseQueryParams(
    val sort: BrowseSort = BrowseSort.Latest,
    val category: Category = Category.All,
)

data class BrowseViewFilters(
    val providers: ImmutableList<ProviderOption> = persistentListOf(),
    val deadTorrents: Boolean = true,
    val hideViewed: Boolean = false,
) {
    data class ProviderOption(
        val provider: String,
        val selected: Boolean = false,
    )
}

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
    savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository,
    private val viewedTorrentRepository: ViewedTorrentRepository,
    private val torrentFileDownloader: TorrentFileDownloader,
) : ViewModel() {
    /**
     * A torrents' loader.
     */
    private val torrentsLoader = TorrentsLoader(
        category = savedStateHandle["category"] ?: Category.All,
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
            contentState,
            torrents,
            viewFilters,
            viewedTorrentHashes,
        ->
        BrowseUiState(
            contentState = contentState,
            torrents = torrents,
            queryParams = queryParams,
            viewFilters = viewFilters,
            viewedTorrentHashes = viewedTorrentHashes,
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

    fun searchTorrents(query: String) {
        torrentsProcessor.searchTorrents(query)
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

    fun toggleSearchProviderResults(providerName: String) {
        torrentsProcessor.toggleSearchProviderResults(providerName)
    }

    fun selectAllSearchProviders() {
        torrentsProcessor.updateExcludedSearchProviders(emptySet())
    }

    fun deselectAllSearchProviders() {
        uiState
            .value
            .viewFilters
            .providers
            .map { it.provider }
            .toSet()
            .let(torrentsProcessor::updateExcludedSearchProviders)
    }

    fun invertSearchProvidersSelection() {
        uiState
            .value
            .viewFilters
            .providers
            .filter { it.selected }
            .map { it.provider }
            .toSet()
            .let(torrentsProcessor::updateExcludedSearchProviders)
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
 *
 * @param category The initial search [Category]
 * @param scope The [CoroutineScope] in which the load is performed.
 * @param searchProvidersGateway The gateway for interacting with providers.
 * @param connectivityChecker The helper class for checking internet connection.
 */
private class TorrentsLoader(
    category: Category,
    private val scope: CoroutineScope,
    private val searchProvidersGateway: SearchProvidersGateway,
    private val connectivityChecker: ConnectivityChecker,
) {
    /**
     * The internal, mutable state of query params.
     *
     * The query params are updated only on-demand and when doing so, a new
     * load job is automatically started to fetch new torrents.
     */
    private val _queryParams = MutableStateFlow(BrowseQueryParams(category = category))

    /**
     * The public, read-only state of the query params.
     */
    val queryParams: StateFlow<BrowseQueryParams> = _queryParams.asStateFlow()

    /**
     * The internal, mutable state of content state.
     * This flow is updated constantly during the load lifecycle.
     */
    private val _state = MutableStateFlow<BrowseContentState>(BrowseContentState.Loading)

    /**
     * The public, read-only state of the content state.
     */
    val state: StateFlow<BrowseContentState> = _state.asStateFlow()

    /**
     * The internal, mutable state of loaded torrents.
     */
    private val _torrents = MutableStateFlow(persistentListOf<Torrent>())

    /**
     * The public, read-only state of raw and unprocessed torrents.
     * Downstream pipelines should use this flow for further processing.
     */
    val torrents: StateFlow<PersistentList<Torrent>> = _torrents.asStateFlow()

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
            _state.value = BrowseContentState.Loading

            if (!connectivityChecker.isInternetAvailable()) {
                _state.value = BrowseContentState.InternetError
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
            _state.value = BrowseContentState.Available.Refreshing

            if (!connectivityChecker.isInternetAvailable()) {
                _state.value = BrowseContentState.Available.Complete
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
            .onCompletion {
                _state.value = if (_torrents.value.isEmpty()) {
                    BrowseContentState.NotAvailable
                } else {
                    BrowseContentState.Available.Complete
                }
            }
            .collect { torrents ->
                _torrents.value = torrents
                _state.value = BrowseContentState.Available.Searching
            }
    }
}

/**
 * Setups and manages the execution of different *intermediate* transformation
 * operations on the [torrents].
 *
 * It is the second stage in the load pipeline which applies view filters and
 * sorting operations.
 *
 * @param torrents The input stream from where torrents are pulled.
 * @param queryParams The [Flow] that emits the current query params.
 *                    Query params are used additionally to remove unwanted torrents.
 * @param viewedTorrentHashes The [Flow] that emits the viewed torrent hashes.
 * @param settingsRepository The repository for fetching user-defined transformation values.
 */
private class TorrentsProcessor(
    torrents: Flow<PersistentList<Torrent>>,
    queryParams: Flow<BrowseQueryParams>,
    viewedTorrentHashes: Flow<Set<String>>,
    settingsRepository: SettingsRepository,
) {
    private data class TorrentFilter(
        val searchQuery: String = "",
        val excludedProviders: Set<String> = emptySet(),
        val deadTorrents: Boolean = true,
        val hideViewed: Boolean = false,
    )

    private val torrentFilter = MutableStateFlow(TorrentFilter())

    /**
     * Names of search provider that are completed successfully.
     */
    private val completedSearchProviders: Flow<Set<String>> =
        torrents.map { it.map { torrent -> torrent.providerName }.toSet() }

    /**
     * The publicly observable, read-only state of the view filters.
     */
    val viewFilters: Flow<BrowseViewFilters> =
        combine(
            torrentFilter,
            completedSearchProviders,
            ::createBrowseViewFilters,
        )

    /**
     * Hashes of currently viewed torrents that should be hidden when filter is active.
     *
     * The hashes are captured only when 'hide viewed' filter is enabled to avoid
     * instant hiding.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentlyViewedTorrentHashes: Flow<Set<String>> =
        torrentFilter
            .map { it.hideViewed }
            .map { if (it) viewedTorrentHashes.firstOrNull().orEmpty() else emptySet() }

    /**
     * The output stream of processed torrents.
     */
    val processedTorrents: Flow<ImmutableList<Torrent>> =
        combine(
            torrents,
            queryParams,
            torrentFilter,
            currentlyViewedTorrentHashes,
            settingsRepository.enableNSFWMode,
            ::processTorrents,
        ).flowOn(Dispatchers.Default)

    /**
     * Creates a [BrowseViewFilters] from the given snapshot of filter config.
     */
    private fun createBrowseViewFilters(
        torrentFilter: TorrentFilter,
        completedSearchProviders: Set<String>,
    ): BrowseViewFilters {
        val providerFilters = completedSearchProviders.map {
            BrowseViewFilters.ProviderOption(
                provider = it,
                selected = it !in torrentFilter.excludedProviders,
            )
        }

        return BrowseViewFilters(
            providers = providerFilters.toImmutableList(),
            deadTorrents = torrentFilter.deadTorrents,
            hideViewed = torrentFilter.hideViewed,
        )
    }

    /**
     * Processes and returns a new list containing processed torrents based
     * on the given filters and other options.
     */
    private fun processTorrents(
        torrents: PersistentList<Torrent>,
        queryParams: BrowseQueryParams,
        torrentFilter: TorrentFilter,
        viewedTorrentHashes: Set<String>,
        nsfwModeEnabled: Boolean,
    ): ImmutableList<Torrent> {
        val sortComparator: Comparator<Torrent> = when (queryParams.sort) {
            BrowseSort.Latest -> compareByDescending { torrent -> torrent.uploadDate }
            BrowseSort.Top -> compareByDescending { torrent -> torrent.seeders }
        }
        val searchQueryWords = torrentFilter.searchQuery
            .split(' ', ignoreCase = true)
            .filter { it.isNotBlank() }

        return torrents.filter {
            add { it.providerName !in torrentFilter.excludedProviders }
            if (!nsfwModeEnabled) add { !it.isNSFW }
            if (!torrentFilter.deadTorrents) add { !it.isDead }
            if (torrentFilter.hideViewed) add { it.infoHash !in viewedTorrentHashes }
            if (torrentFilter.searchQuery.isNotBlank()) add {
                searchQueryWords.any { word -> it.name.contains(word, ignoreCase = true) }
            }
            if (queryParams.category != Category.All) add { queryParams.category == it.category }
        }
            .sortedWithComparator(sortComparator)
            .toImmutableList()
    }

    fun searchTorrents(query: String) {
        torrentFilter.update { it.copy(searchQuery = query) }
    }

    /**
     * Shows or hides search results associated with the given provider name.
     */
    fun toggleSearchProviderResults(providerName: String) {
        torrentFilter.update {
            val newExclusions = if (providerName in it.excludedProviders) {
                // Remove from exclusion list.
                it.excludedProviders - providerName
            } else {
                // Exclude it.
                it.excludedProviders + providerName
            }
            it.copy(excludedProviders = newExclusions)
        }
    }

    /**
     * Updates the current search providers exclusion list with the given one.
     */
    fun updateExcludedSearchProviders(providers: Set<String>) {
        torrentFilter.update { it.copy(excludedProviders = providers) }
    }

    /**
     * Shows or hides dead torrents.
     */
    fun toggleDeadTorrents() {
        torrentFilter.update { it.copy(deadTorrents = !it.deadTorrents) }
    }

    /**
     * Toggles the "hide viewed" filter.
     */
    fun toggleHideViewed() {
        torrentFilter.update { it.copy(hideViewed = !it.hideViewed) }
    }
}