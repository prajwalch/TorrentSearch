package com.prajwalch.torrentsearch.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarkRepository
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.ViewedTorrentRepository
import com.prajwalch.torrentsearch.domain.SearchTorrentsUseCase
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SearchResults
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.torrentfiledownloader.TorrentFileDownloadEvent
import com.prajwalch.torrentsearch.torrentfiledownloader.TorrentFileDownloadState
import com.prajwalch.torrentsearch.torrentfiledownloader.TorrentFileDownloader
import com.prajwalch.torrentsearch.util.createSortComparator

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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
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

data class SearchUiState(
    val searchQuery: String = "",
    val searchCategory: Category = Category.All,
    val searchResults: SearchResults = SearchResults(),
    val sortOptions: SortOptions = SortOptions(),
    val filterOptions: FilterOptions = FilterOptions(),
    val viewedTorrentHashes: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val isInternetError: Boolean = false,
    val resultsNotFound: Boolean = false,
    val resultsFilteredOut: Boolean = false,
)

data class FilterOptions(
    val searchProviders: ImmutableList<SearchProviderFilterOption> = persistentListOf(),
    val deadTorrents: Boolean = true,
    val category: Category = Category.All,
    val hideViewed: Boolean = false,
)

data class SearchProviderFilterOption(
    val searchProviderName: String,
    val selected: Boolean = false,
)

/**
 * A ViewModel that handles the business logic of search screen.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTorrentsUseCase: SearchTorrentsUseCase,
    private val bookmarkRepository: BookmarkRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val viewedTorrentRepository: ViewedTorrentRepository,
    private val connectivityChecker: ConnectivityChecker,
    private val torrentFileDownloader: TorrentFileDownloader,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * Current search query.
     */
    private val searchQuery: String = savedStateHandle["query"]
        ?: error("SearchViewModel can't function without a search query")

    /**
     * Current search category.
     */
    private val searchCategory: Category = savedStateHandle["category"] ?: Category.All

    /**
     * The [SearchOrchestrator] responsible for coordinating search task
     * and managing search related state.
     */
    private val searchOrchestrator = SearchOrchestrator(
        scope = viewModelScope,
        searchTorrentsUseCase = searchTorrentsUseCase,
        connectivityChecker = connectivityChecker,
    )

    /**
     * The search results processor responsible for filtering, sorting, and
     * managing the related state.
     */
    private val resultsProcessor = SearchResultsProcessor(
        searchResults = searchOrchestrator.searchResults,
        settingsRepository = settingsRepository,
        viewedTorrentHashes = viewedTorrentRepository.getAllViewedHashes(),
        initialSelectedCategory = searchCategory,
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

    /**
     * The primary, read-only UI state.
     */
    val uiState = combine(
        searchOrchestrator.searchState,
        resultsProcessor.processorState,
        resultsProcessor.processedSearchResults,
        resultsProcessor.availableSearchProviders,
        viewedTorrentRepository.getAllViewedHashes(),
    ) {
            searchState,
            processorState,
            processedResults,
            availableSearchProviders,
            viewedTorrentHashes,
        ->
        val resultsFilteredOut = when {
            searchState.isSearching -> false
            searchState.isRefreshing -> false
            searchState.resultsNotFound -> false
            else -> processedResults.successes.isEmpty()
        }
        val filterOptions = createFilterOptions(
            providers = availableSearchProviders,
            filters = processorState.filters,
        )

        SearchUiState(
            searchQuery = searchQuery,
            searchCategory = searchCategory,
            searchResults = processedResults,
            sortOptions = processorState.sortOptions,
            filterOptions = filterOptions,
            viewedTorrentHashes = viewedTorrentHashes,
            isLoading = searchState.isLoading,
            isSearching = searchState.isSearching,
            isRefreshing = searchState.isRefreshing,
            isInternetError = searchState.isInternetError,
            resultsNotFound = searchState.resultsNotFound,
            resultsFilteredOut = resultsFilteredOut,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SearchUiState(isLoading = true),
    )

    init {
        saveSearchQuery()
        // Initiate search.
        viewModelScope.launch {
            val defaultSortOptions = settingsRepository.defaultSortOptions.first()
            resultsProcessor.updateSortCriteria(defaultSortOptions.criteria)
            resultsProcessor.updateSortOrder(defaultSortOptions.order)

            search()
        }
    }

    /**
     * Creates a [FilterOptions] from the given list of search provider name
     * and the filter values used by the [SearchResultsProcessor] during search
     * results processing.
     */
    private fun createFilterOptions(
        providers: Set<String>,
        filters: SearchResultsProcessor.Filters,
    ): FilterOptions {
        val searchProvidersFilterOption = providers.map {
            SearchProviderFilterOption(
                searchProviderName = it,
                selected = it !in filters.excludedProviders,
            )
        }

        return FilterOptions(
            searchProviders = searchProvidersFilterOption.toImmutableList(),
            deadTorrents = filters.deadTorrents,
            category = filters.category,
            hideViewed = filters.hideViewed,
        )
    }

    private fun saveSearchQuery() = viewModelScope.launch {
        if (settingsRepository.saveSearchHistory.first()) {
            searchHistoryRepository.createNewSearchHistory(query = searchQuery)
        }
    }

    fun search() {
        searchOrchestrator.search(searchQuery, searchCategory)
    }

    fun refreshSearchResults() {
        searchOrchestrator.refresh(searchQuery, searchCategory)
    }

    fun filterSearchResults(query: String) {
        resultsProcessor.updateFilterQuery(query)
    }

    fun updateSortCriteria(criteria: SortCriteria) {
        resultsProcessor.updateSortCriteria(criteria)
    }

    fun updateSortOrder(order: SortOrder) {
        resultsProcessor.updateSortOrder(order)
    }

    fun toggleSearchProviderResults(providerName: String) {
        resultsProcessor.toggleSearchProviderResults(providerName)
    }

    fun selectAllSearchProviders() {
        resultsProcessor.updateExcludedSearchProviders(emptySet())
    }

    fun deselectAllSearchProviders() {
        uiState
            .value
            .filterOptions
            .searchProviders
            .map { it.searchProviderName }
            .toSet()
            .let(resultsProcessor::updateExcludedSearchProviders)
    }

    fun invertSearchProvidersSelection() {
        uiState
            .value
            .filterOptions
            .searchProviders
            .filter { it.selected }
            .map { it.searchProviderName }
            .toSet()
            .let(resultsProcessor::updateExcludedSearchProviders)
    }

    fun toggleDeadTorrents() {
        resultsProcessor.toggleDeadTorrents()
    }

    fun updateCategoryFilter(category: Category) {
        resultsProcessor.updateCategory(category)
    }

    fun toggleHideViewedTorrents() {
        resultsProcessor.toggleHideViewed()
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

/**
 * Manages and coordinates the search task.
 *
 * It is the first stage in the pipeline which handles the task of producing
 * search results to consume for other stages.
 */
private class SearchOrchestrator(
    /**
     * The [CoroutineScope] in which the search is performed.
     */
    private val scope: CoroutineScope,
    /**
     * A use case which handles the actual search task.
     */
    private val searchTorrentsUseCase: SearchTorrentsUseCase,
    /**
     * A helper class for checking network condition.
     */
    private val connectivityChecker: ConnectivityChecker,
) {
    /**
     * Represents the current state of search.
     */
    data class SearchState(
        val isLoading: Boolean = true,
        val isSearching: Boolean = false,
        val isRefreshing: Boolean = false,
        val isInternetError: Boolean = false,
        val resultsNotFound: Boolean = false,
    )

    /**
     * The internal, mutable source of truth for the [SearchState].
     * This flow is updated constantly during the search lifecycle.
     */
    private val _searchState = MutableStateFlow(SearchState())

    /**
     * The publicly observable, read-only state of the [SearchState].
     */
    val searchState = _searchState.asStateFlow()

    /**
     * The internal, mutable source of truth for the search results.
     */
    private val _searchResults = MutableStateFlow(SearchResults())

    /**
     * The publicly observable, read-only state of raw and unprocessed
     * search results.
     */
    val searchResults = _searchResults.asStateFlow()

    /**
     * An ongoing background search job.
     *
     * Before starting a new lifecycle the ongoing search is explicitly canceled
     * whether it has completed or not to prevent unnecessary usage of resource.
     */
    private var searchJob: Job? = null

    /**
     * Initiates a new search task for the given query and category.
     */
    fun search(query: String, category: Category) {
        searchJob?.cancel()
        searchJob = scope.launch {
            _searchState.value = SearchState(isLoading = true)

            if (!connectivityChecker.isInternetAvailable()) {
                _searchState.update { it.copy(isLoading = false, isInternetError = true) }
                return@launch
            }

            executeSearch(query = query, category = category)
        }
    }

    /**
     * Initiates a refresh task for the given query and category.
     */
    fun refresh(query: String, category: Category) {
        searchJob?.cancel()
        searchJob = scope.launch {
            _searchState.update { it.copy(isRefreshing = true) }

            if (!connectivityChecker.isInternetAvailable()) {
                _searchState.update { it.copy(isRefreshing = false) }
                return@launch
            }

            executeSearch(query = query, category = category)
        }
    }

    /**
     * Executes a search for the given query and category, updating
     * [_searchState] throughout the entire search lifecycle.
     */
    private suspend fun executeSearch(query: String, category: Category) {
        searchTorrentsUseCase(query = query, category = category)
            .conflate()
            .onStart { onSearchStart() }
            .onCompletion { onSearchCompletion() }
            .collect { _searchResults.value = it }
    }

    /**
     * Invoked when search starts.
     */
    private fun onSearchStart() {
        _searchState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                isSearching = true,
            )
        }
    }

    /**
     * Invoked when search completes either normally or abnormally.
     */
    private fun onSearchCompletion() {
        _searchState.update {
            it.copy(
                isSearching = false,
                resultsNotFound = _searchResults.value.successes.isEmpty(),
            )
        }
    }
}

/**
 * Setups and manages the execution of different *intermediate* transformation
 * operations on the [searchResults].
 *
 * It is the second stage in the search pipeline which handles filtering and
 * sorting operations on the search results.
 */
private class SearchResultsProcessor(
    /**
     * The asynchronous input stream from where [SearchResults] are pulled.
     */
    searchResults: Flow<SearchResults>,
    /**
     * The repository for fetching the user-defined transformation options.
     */
    settingsRepository: SettingsRepository,
    /**
     * Flow of viewed torrent hashes for filtering.
     */
    viewedTorrentHashes: Flow<Set<String>>,
    /**
     * The [Category] to use as an initial value for [Filters.category].
     */
    initialSelectedCategory: Category = Category.All,
) {
    /**
     * Represents the current state of processor.
     */
    data class ProcessorState(
        val filters: Filters,
        val sortOptions: SortOptions,
    )

    /**
     * Represents the different values and options of filters.
     */
    data class Filters(
        val query: String = "",
        val excludedProviders: Set<String> = emptySet(),
        val deadTorrents: Boolean = true,
        val category: Category = Category.All,
        val hideViewed: Boolean = false,
    )

    /**
     * The internal mutable source for filters.
     */
    private val filters = MutableStateFlow(Filters(category = initialSelectedCategory))

    /**
     * The internal mutable source for sort options.
     */
    private val sortOptions = MutableStateFlow(SortOptions())

    /**
     * The publicly observable state of the processor.
     */
    val processorState: Flow<ProcessorState> = combine(filters, sortOptions, ::ProcessorState)

    /**
     * Hashes of currently viewed torrents that should be hidden when filter is active.
     *
     * The hashes are captured only when 'hide viewed' filter is enabled to avoid
     * instant hiding.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentlyViewedTorrentHashes: Flow<Set<String>> =
        filters
            .map { it.hideViewed }
            .map { if (it) viewedTorrentHashes.firstOrNull().orEmpty() else emptySet() }

    /**
     * The asynchronous output stream of processed search results.
     */
    val processedSearchResults: Flow<SearchResults> =
        combine(
            searchResults,
            filters,
            sortOptions,
            settingsRepository.enableNSFWMode,
            currentlyViewedTorrentHashes,
            ::processSearchResults
        ).flowOn(Dispatchers.Default)

    /**
     * List of search providers name extracted from the search results.
     */
    val availableSearchProviders: Flow<Set<String>> = searchResults.map {
        it.successes.map { torrent -> torrent.providerName }.toSet()
    }

    /**
     * Processes and returns a new search results based on the given values.
     */
    private fun processSearchResults(
        rawSearchResults: SearchResults,
        filters: Filters,
        sortOptions: SortOptions,
        nsfwModeEnabled: Boolean,
        viewedTorrentHashes: Set<String>,
    ): SearchResults {
        val sortComparator = createSortComparator(
            criteria = sortOptions.criteria,
            order = sortOptions.order,
        )
        val processedSuccesses = rawSearchResults
            .successes
            .asSequence()
            .filterNot { it.providerName in filters.excludedProviders }
            .filter { nsfwModeEnabled || !it.isNSFW }
            .filter { filters.deadTorrents || !it.isDead }
            .filter { !filters.hideViewed || it.infoHash !in viewedTorrentHashes }
            .filter { filters.query.isBlank() || it.name.contains(filters.query, true) }
            .filter { filters.category == Category.All || filters.category == it.category }
            .sortedWith(comparator = sortComparator)
            .toImmutableList()

        return SearchResults(
            successes = processedSuccesses,
            failures = rawSearchResults.failures,
        )
    }

    /**
     * Shows only those search results that contains the given query.
     */
    fun updateFilterQuery(query: String) {
        filters.update { it.copy(query = query.trim()) }
    }

    /**
     * Shows or hides search results associated with the given provider name.
     */
    fun toggleSearchProviderResults(providerName: String) {
        filters.update {
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
        filters.update { it.copy(excludedProviders = providers) }
    }

    /**
     * Shows or hides dead torrents from search results.
     */
    fun toggleDeadTorrents() {
        filters.update { it.copy(deadTorrents = !it.deadTorrents) }
    }

    /**
     * Updates the current category with the given one.
     */
    fun updateCategory(category: Category) {
        filters.update { it.copy(category = category) }
    }

    /**
     * Updates the current sort criteria with the given one.
     */
    fun updateSortCriteria(criteria: SortCriteria) {
        sortOptions.update { it.copy(criteria = criteria) }
    }

    /**
     * Updates the current sort order with the given one.
     */
    fun updateSortOrder(order: SortOrder) {
        sortOptions.update { it.copy(order = order) }
    }

    /**
     * Toggles the hide viewed filter.
     */
    fun toggleHideViewed() {
        filters.update { it.copy(hideViewed = !it.hideViewed) }
    }
}