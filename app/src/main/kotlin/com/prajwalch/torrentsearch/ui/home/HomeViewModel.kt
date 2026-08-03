package com.prajwalch.torrentsearch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SearchHistory

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class HomeUiState(
    val histories: List<SearchHistory> = emptyList(),
    val categories: List<Category> = Category.entries,
    val selectedCategory: Category = Category.All,
    val searchHistoryEnabled: Boolean = true,
    val searchProvidersInitialized: Boolean = false,
)

/**
 * The ViewModel which handles the business logic of home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    searchHistoryRepository: SearchHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val searchProvidersManager: SearchProvidersManager,
) : ViewModel() {
    /**
     * The internal source for the current search query used only for
     * filtering search histories.
     *
     * UI maintains the query by itself but notifies the ViewModel whenever the
     * query changes. We then update this flow with copy of the query.
     */
    private val searchQuery = MutableStateFlow("")

    /**
     * The primary asynchronous stream of [SearchHistory].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchHistories: Flow<List<SearchHistory>> =
        combine(
            searchQuery,
            settingsRepository.showSearchHistory,
            ::Pair,
        ).flatMapLatest { (query, showSearchHistory) ->
            when {
                // Avoid fetching histories when not needed.
                !showSearchHistory -> flowOf(emptyList())
                query.isBlank() -> searchHistoryRepository.getAllSearchHistories()
                else -> searchHistoryRepository.getSearchHistoriesByTerm(query)
            }
        }

    /**
     * The primary asynchronous stream of selectable [Category].
     */
    private val selectableCategories: Flow<List<Category>> =
        settingsRepository.enableNSFWMode.map { nsfwModeEnabled ->
            Category.entries.filter { nsfwModeEnabled || !it.isNSFW }
        }

    /**
     * The internal, primary mutable source for the currently selected
     * category. The flow is updated on demand from the UI.
     */
    private val selectedCategory = MutableStateFlow(Category.All)

    /**
     * The primary read-only UI state.
     */
    val uiState = combine(
        searchHistories,
        selectableCategories,
        selectedCategory,
        settingsRepository.saveSearchHistory,
        settingsRepository.searchProvidersInitialized,
    ) {
            histories,
            selectableCategories,
            selectedCategory,
            searchHistoryEnabled,
            searchProvidersInitialized,
        ->
        val selectedCategory = when {
            selectedCategory in selectableCategories -> selectedCategory
            else -> Category.All
        }

        HomeUiState(
            histories = histories,
            categories = selectableCategories,
            selectedCategory = selectedCategory,
            searchHistoryEnabled = searchHistoryEnabled,
            searchProvidersInitialized = searchProvidersInitialized,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = HomeUiState(),
    )

    init {
        loadDefaultCategory()
    }

    /**
     * Loads the user-defined default category.
     */
    private fun loadDefaultCategory() = viewModelScope.launch {
        val defaultCategory = settingsRepository.defaultCategory.firstOrNull()

        if (defaultCategory != null) {
            selectedCategory.value = defaultCategory
        }
    }

    /**
     * Sets the currently selected category to given one.
     */
    fun setCategory(category: Category) {
        selectedCategory.value = category
    }

    /**
     * Filters search histories by the given query.
     */
    fun filterSearchHistories(query: String) {
        searchQuery.value = query
    }

    fun enableDefaultSearchProviders() {
        viewModelScope.launch {
            searchProvidersManager.enableDefaultSearchProviders()
        }
    }

    fun skipDefaultSearchProviders() {
        viewModelScope.launch {
            searchProvidersManager.skipDefaultSearchProviders()
        }
    }
}