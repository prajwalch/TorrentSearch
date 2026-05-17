package com.prajwalch.torrentsearch.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.BookmarkRepository
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.ViewedTorrentRepository
import com.prajwalch.torrentsearch.domain.SearchTorrentsUseCase
import com.prajwalch.torrentsearch.domain.TorrentFileDownloadEvent
import com.prajwalch.torrentsearch.domain.TorrentFileDownloadState
import com.prajwalch.torrentsearch.domain.TorrentFileDownloader
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SearchResults
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.domain.model.SortOrder
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.filterSuccesses
import com.prajwalch.torrentsearch.domain.model.sortSuccessesWith
import com.prajwalch.torrentsearch.network.ConnectivityChecker
import com.prajwalch.torrentsearch.util.createSortComparator

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.io.OutputStream
import javax.inject.Inject

import kotlin.time.Duration.Companion.seconds

data class SearchUiState(
    val searchParams: SearchParams = SearchParams(),
    val searchState: SearchState = SearchState.Loading,
    val searchResults: SearchResults = SearchResults(),
    val sortOptions: SortOptions = SortOptions(),
    val torrentFilter: TorrentFilter = TorrentFilter(),
    val viewedTorrentHashes: Set<String> = emptySet(),
)

data class SearchParams(
    val query: String = "",
    val category: Category = Category.All,
)

sealed interface SearchState {
    data object Loading : SearchState
    data object InternetError : SearchState
    data object ResultsNotFound : SearchState

    sealed interface ResultsAvailable : SearchState {
        data object Complete : ResultsAvailable
        data object Searching : ResultsAvailable
        data object Refreshing : ResultsAvailable
    }
}

data class TorrentFilter(
    val providers: ImmutableList<SearchProviderOption> = persistentListOf(),
    val showDeadTorrents: Boolean = true,
    val category: Category = Category.All,
    val hideViewed: Boolean = false,
) {
    data class SearchProviderOption(
        val provider: String,
        val selected: Boolean = false,
    )
}

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
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * The search params set by the user.
     *
     * Search params are final and not allowed to modify or update for
     * requesting a new search with new params.
     */
    private val searchParams = getSearchParams()

    /**
     * The search results loader.
     *
     * It produces raw and unprocessed results, which are directly feed into
     * the processor for post-processing such as filtering and sorting.
     */
    private val resultsLoader = SearchResultsLoader(
        scope = viewModelScope,
        searchTorrentsUseCase = searchTorrentsUseCase,
        connectivityChecker = connectivityChecker,
    )

    /**
     * The search results processor.
     *
     * It pulls search results from the given flow, does some post-processing
     * and produces another results, which are finally sent to the UI.
     */
    private val resultsProcessor = SearchResultsProcessor(
        searchResults = resultsLoader.searchResults,
        settingsRepository = settingsRepository,
        viewedTorrentHashes = viewedTorrentRepository.getAllViewedHashes(),
        initialSelectedCategory = searchParams.category,
    )

    /**
     * The primary, read-only UI state.
     */
    val uiState: StateFlow<SearchUiState> =
        combine(
            resultsLoader.searchState,
            resultsProcessor.processedSearchResults,
            resultsProcessor.sortOptions,
            resultsProcessor.torrentFilter,
            viewedTorrentRepository.getAllViewedHashes(),
        ) {
                searchState,
                processedResults,
                sortOptions,
                viewFilters,
                viewedTorrentHashes,
            ->
            SearchUiState(
                searchParams = searchParams,
                searchState = searchState,
                searchResults = processedResults,
                sortOptions = sortOptions,
                torrentFilter = viewFilters,
                viewedTorrentHashes = viewedTorrentHashes,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SearchUiState(searchState = SearchState.Loading),
        )

    /**
     * The torrent file download state.
     */
    val torrentFileDownloadState: StateFlow<TorrentFileDownloadState> =
        torrentFileDownloader.state

    /**
     * A stream of torrent file download event.
     */
    val torrentFileDownloadEvents: Flow<TorrentFileDownloadEvent> =
        torrentFileDownloader.events

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

    @Throws(IllegalStateException::class)
    private fun getSearchParams(): SearchParams {
        val query = savedStateHandle.get<String>("query")
            // Accepting the keyboard suggestion will add extra trailing space.
            // By removing it here, the query remains clean for both history save
            // and search task.
            ?.trim()
        // TODO: Support empty search query.
            ?: error("Empty search query isn't supported")
        val category = savedStateHandle.get<Category>("category") ?: Category.All

        return SearchParams(query, category)
    }

    private fun saveSearchQuery() = viewModelScope.launch {
        if (settingsRepository.saveSearchHistory.first()) {
            searchHistoryRepository.createNewSearchHistory(query = searchParams.query)
        }
    }

    fun search() {
        resultsLoader.search(searchParams.query, searchParams.category)
    }

    fun stopSearch() {
        resultsLoader.stopSearch()
    }

    fun refreshSearchResults() {
        resultsLoader.refresh(searchParams.query, searchParams.category)
    }

    fun filterSearchResultsByName(query: String) {
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
            .torrentFilter
            .providers
            .map { it.provider }
            .toSet()
            .let(resultsProcessor::updateExcludedSearchProviders)
    }

    fun invertSearchProvidersSelection() {
        uiState
            .value
            .torrentFilter
            .providers
            .filter { it.selected }
            .map { it.provider }
            .toSet()
            .let(resultsProcessor::updateExcludedSearchProviders)
    }

    fun toggleDeadTorrents() {
        resultsProcessor.toggleShowDeadTorrents()
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
 * Manages and handles the search results fetching task.
 *
 * It's the first stage in the search pipeline which is responsible for
 * maintaining and producing search state and results. As the name suggests,
 * it doesn't perform any pre-processing on the results.
 *
 * @param scope The [CoroutineScope] in which the search is performed.
 * @param searchTorrentsUseCase The use-case that performs the search.
 * @param connectivityChecker The helper class for checking network connection.
 */
private class SearchResultsLoader(
    private val scope: CoroutineScope,
    private val searchTorrentsUseCase: SearchTorrentsUseCase,
    private val connectivityChecker: ConnectivityChecker,
) {
    /**
     * The internal, mutable state of [SearchState].
     * This flow is updated constantly during the search lifecycle.
     */
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Loading)

    /**
     * The public, read-only state of the [SearchState].
     */
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    /**
     * The internal, mutable state of current search results.
     */
    private val _searchResults = MutableStateFlow(SearchResults())

    /**
     * The public, read-only state of unprocessed search results.
     */
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()

    /**
     * The ongoing background search job.
     *
     * Before starting a new lifecycle current job is explicitly canceled
     * whether it has completed or not to prevent unnecessary usage of resource.
     */
    private var searchJob: Job? = null

    /**
     * Initiates a new search for the given query and category.
     */
    fun search(query: String, category: Category) {
        searchJob?.cancel()
        searchJob = scope.launch {
            _searchState.value = SearchState.Loading

            if (!connectivityChecker.isInternetAvailable()) {
                _searchState.value = SearchState.InternetError
                return@launch
            }

            executeSearch(query = query, category = category)
        }
    }

    /**
     * Stops the ongoing search.
     */
    fun stopSearch() {
        searchJob?.cancel()
        searchJob = null
    }

    /**
     * Refreshes the current search results by refetching new results.
     */
    fun refresh(query: String, category: Category) {
        searchJob?.cancel()
        searchJob = scope.launch {
            _searchState.value = SearchState.ResultsAvailable.Refreshing

            if (!connectivityChecker.isInternetAvailable()) {
                _searchState.value = SearchState.ResultsAvailable.Complete
                return@launch
            }

            executeSearch(query = query, category = category)
        }
    }

    /**
     * Executes a new search for the given query and category.
     */
    private suspend fun executeSearch(query: String, category: Category) {
        searchTorrentsUseCase(query = query, category = category)
            .conflate()
            .onCompletion {
                _searchState.value = if (_searchResults.value.successes.isEmpty()) {
                    SearchState.ResultsNotFound
                } else {
                    SearchState.ResultsAvailable.Complete
                }
            }
            .collect {
                _searchResults.value = it
                _searchState.value = SearchState.ResultsAvailable.Searching
            }
    }
}

/**
 * Setups and manages the execution of different transformation
 * operations on the [searchResults].
 *
 * It's the second stage in the search pipeline, responsible for performing
 * post-processing on the [searchResults], maintaining and producing
 * related states and processed search results.
 *
 * @param searchResults The flow that emits the [SearchResults].
 * @param settingsRepository The repository from where user-define filter options are fetched.
 * @param viewedTorrentHashes The flow that emits the viewed torrent hashes.
 * @param initialSelectedCategory The [Category] to use as an initial value for filter.
 */
private class SearchResultsProcessor(
    searchResults: Flow<SearchResults>,
    settingsRepository: SettingsRepository,
    viewedTorrentHashes: Flow<Set<String>>,
    initialSelectedCategory: Category = Category.All,
) {
    /**
     * A snapshot of the current configuration of [TorrentFilter].
     */
    private data class TorrentFilterConfig(
        val query: String = "",
        val excludedProviders: Set<String> = emptySet(),
        val showDeadTorrents: Boolean = true,
        val category: Category = Category.All,
        val hideViewed: Boolean = false,
    )

    /**
     * The internal, mutable state of [TorrentFilterConfig].
     */
    private val torrentFilterConfig =
        MutableStateFlow(TorrentFilterConfig(category = initialSelectedCategory))

    /**
     * Names of search provider that are completed successfully.
     */
    private val completedSearchProviders: Flow<Set<String>> =
        searchResults.map {
            it.successes.map { torrent -> torrent.providerName }.toSet()
        }

    /**
     * The flow that emits the [TorrentFilter].
     */
    val torrentFilter: Flow<TorrentFilter> =
        combine(
            torrentFilterConfig,
            completedSearchProviders,
            ::createTorrentFilter,
        )

    /**
     * The internal, mutable state of sort options.
     */
    private val _sortOptions = MutableStateFlow(SortOptions())

    /**
     * The public, read-only state of sort options.
     */
    val sortOptions: StateFlow<SortOptions> = _sortOptions.asStateFlow()

    /**
     * Hashes of currently viewed torrents that should be hidden when
     * 'hide viewed' filter is turned on.
     *
     * The hashes are captured only when 'hide viewed' filter is enabled to avoid
     * instant hiding.
     */
    private val currentlyViewedTorrentHashes: Flow<Set<String>> =
        torrentFilterConfig
            .map { it.hideViewed }
            .map { if (it) viewedTorrentHashes.firstOrNull().orEmpty() else emptySet() }

    /**
     * The flow that emits the processed [SearchResults].
     */
    val processedSearchResults: Flow<SearchResults> =
        combine(
            searchResults,
            torrentFilterConfig,
            _sortOptions,
            settingsRepository.enableNSFWMode,
            currentlyViewedTorrentHashes,
            ::processSearchResults
        ).flowOn(Dispatchers.Default)

    /**
     * Creates a [TorrentFilter] from the given snapshot of filter config.
     */
    private fun createTorrentFilter(
        filterConfig: TorrentFilterConfig,
        completedSearchProviders: Set<String>,
    ): TorrentFilter {
        val providerFilters = completedSearchProviders.map {
            TorrentFilter.SearchProviderOption(
                provider = it,
                selected = it !in filterConfig.excludedProviders,
            )
        }

        return TorrentFilter(
            providers = providerFilters.toImmutableList(),
            showDeadTorrents = filterConfig.showDeadTorrents,
            category = filterConfig.category,
            hideViewed = filterConfig.hideViewed,
        )
    }

    /**
     * Processes the [searchResults] using given configurations and returns a
     * new [SearchResults].
     */
    private fun processSearchResults(
        searchResults: SearchResults,
        filterConfig: TorrentFilterConfig,
        sortOptions: SortOptions,
        nsfwModeEnabled: Boolean,
        viewedTorrentHashes: Set<String>,
    ): SearchResults {
        val sortComparator = createSortComparator(
            criteria = sortOptions.criteria,
            order = sortOptions.order,
        )
        val filterQueryWords = filterConfig.query
            .split(' ', ignoreCase = true)
            .filter { it.isNotBlank() }

        return searchResults.filterSuccesses {
            add { it.providerName !in filterConfig.excludedProviders }
            if (!nsfwModeEnabled) add { !it.isNSFW }
            if (!filterConfig.showDeadTorrents) add { !it.isDead }
            if (filterConfig.hideViewed) add { it.infoHash !in viewedTorrentHashes }
            if (filterConfig.query.isNotBlank()) add {
                filterQueryWords.any { word -> it.name.contains(word, ignoreCase = true) }
            }
            if (filterConfig.category != Category.All) add { filterConfig.category == it.category }
        }.sortSuccessesWith(sortComparator)
    }

    /**
     * Shows only those search results that contains the given query.
     */
    fun updateFilterQuery(query: String) {
        torrentFilterConfig.update { it.copy(query = query.trim()) }
    }

    /**
     * Shows or hides search results associated with the given provider name.
     */
    fun toggleSearchProviderResults(providerName: String) {
        torrentFilterConfig.update {
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
        torrentFilterConfig.update { it.copy(excludedProviders = providers) }
    }

    /**
     * Shows or hides dead torrents from search results.
     */
    fun toggleShowDeadTorrents() {
        torrentFilterConfig.update { it.copy(showDeadTorrents = !it.showDeadTorrents) }
    }

    /**
     * Updates the current category with the given one.
     */
    fun updateCategory(category: Category) {
        torrentFilterConfig.update { it.copy(category = category) }
    }

    /**
     * Toggles the hide viewed filter.
     */
    fun toggleHideViewed() {
        torrentFilterConfig.update { it.copy(hideViewed = !it.hideViewed) }
    }

    /**
     * Updates the current sort criteria with the given one.
     */
    fun updateSortCriteria(criteria: SortCriteria) {
        _sortOptions.update { it.copy(criteria = criteria) }
    }

    /**
     * Updates the current sort order with the given one.
     */
    fun updateSortOrder(order: SortOrder) {
        _sortOptions.update { it.copy(order = order) }
    }
}