package com.prajwalch.torrentsearch.ui.settings

import android.content.ComponentName
import android.content.pm.PackageManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.BuildConfig
import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.DarkTheme
import com.prajwalch.torrentsearch.domain.models.MaxNumResults
import com.prajwalch.torrentsearch.domain.models.SortOptions

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.io.OutputStream
import java.io.PrintWriter

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class SettingsUiState(
    val appearanceSettings: AppearanceSettingsUiState = AppearanceSettingsUiState(),
    val generalSettings: GeneralSettingsUiState = GeneralSettingsUiState(),
    val searchSettings: SearchSettingsUiState = SearchSettingsUiState(),
    val searchHistorySettings: SearchHistorySettingsUiState = SearchHistorySettingsUiState(),
    val advancedSettings: AdvancedSettingsUiState = AdvancedSettingsUiState(),
)

data class AppearanceSettingsUiState(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val pureBlack: Boolean = false,
)

data class GeneralSettingsUiState(
    val enableNSFWMode: Boolean = false,
)

data class SearchSettingsUiState(
    val searchProvidersStat: SearchProvidersStat = SearchProvidersStat(),
    val defaultCategory: Category = Category.All,
    val defaultSortOptions: SortOptions = SortOptions(),
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
) {
    data class SearchProvidersStat(
        val enabledSearchProvidersCount: Int = 0,
        val totalSearchProvidersCount: Int = 0,
    )
}

data class SearchHistorySettingsUiState(
    val saveSearchHistory: Boolean = true,
    val showSearchHistory: Boolean = true,
)

data class AdvancedSettingsUiState(
    val enableShareIntegration: Boolean = true,
    val enableQuickSearch: Boolean = true,
)

/** ViewModel that handles the business logic of Settings screen. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val searchProvidersRepository: SearchProvidersRepository,
) : ViewModel() {
    val uiState = getSettingsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SettingsUiState(),
    )

    private fun getSettingsFlow(): Flow<SettingsUiState> {
        val appearanceSettingsFlow = combine(
            settingsRepository.enableDynamicTheme,
            settingsRepository.darkTheme,
            settingsRepository.pureBlack,
            ::AppearanceSettingsUiState,
        )
        val generalSettingsFlow = settingsRepository
            .enableNSFWMode
            .map(::GeneralSettingsUiState)

        val searchProvidersStatFlow = combine(
            settingsRepository.enabledSearchProvidersId.map { it.size },
            searchProvidersRepository.observeSearchProvidersCount(),
            SearchSettingsUiState::SearchProvidersStat,
        )
        val searchSettingsFlow = combine(
            searchProvidersStatFlow,
            settingsRepository.defaultCategory,
            settingsRepository.defaultSortOptions,
            settingsRepository.maxNumResults,
            ::SearchSettingsUiState,
        )

        val searchHistorySettingsFlow = combine(
            settingsRepository.saveSearchHistory,
            settingsRepository.showSearchHistory,
            ::SearchHistorySettingsUiState,
        )
        val advancedSettingsFlow = combine(
            settingsRepository.enableShareIntegration,
            settingsRepository.enableQuickSearch,
            ::AdvancedSettingsUiState,
        )

        return combine(
            appearanceSettingsFlow,
            generalSettingsFlow,
            searchSettingsFlow,
            searchHistorySettingsFlow,
            advancedSettingsFlow,
            ::SettingsUiState,
        )
    }

    /** Enables/disables dynamic theme. */
    fun enableDynamicTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableDynamicTheme(enable = enable)
        }
    }

    /** Changes the dark theme mode. */
    fun setDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(darkTheme = darkTheme)
        }
    }

    /** Enables/disables pure black mode. */
    fun enablePureBlackTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enablePureBlack(enable = enable)
        }
    }

    /** Enables/disables NSFW mode. */
    fun enableNSFWMode(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableNSFWMode(enable = enable)
            if (!enable) disableRestrictedSearchProviders()
        }
    }

    /** Disables NSFW and Unsafe search providers which are currently enabled. */
    private suspend fun disableRestrictedSearchProviders() {
        val enabledSearchProvidersInfo = combine(
            searchProvidersRepository.observeSearchProvidersInfo(),
            settingsRepository.enabledSearchProvidersId,
        ) { searchProvidersInfo, enabledSearchProvidersId ->
            searchProvidersInfo.filter { it.id in enabledSearchProvidersId }
        }.firstOrNull() ?: return

        val newEnabledSearchProvidersId = enabledSearchProvidersInfo
            .filterNot { it.specializedCategory.isNSFW || it.safetyStatus.isUnsafe() }
            .map { it.id }
            .toSet()

        settingsRepository.setEnabledSearchProvidersId(
            providersId = newEnabledSearchProvidersId,
        )
    }

    /** Updates the maximum number of results. */
    fun setMaxNumResults(maxNumResults: MaxNumResults) {
        viewModelScope.launch {
            settingsRepository.setMaxNumResults(maxNumResults = maxNumResults)
        }
    }

    /** Saves/unsaves search history. */
    fun enableSaveSearchHistory(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableSaveSearchHistory(enable = save)
        }
    }

    /** Shows/hides search history. */
    fun enableShowSearchHistory(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableShowSearchHistory(show = show)
        }
    }

    fun enableShareIntegration(enable: Boolean, packageManager: PackageManager) {
        viewModelScope.launch {
            enableIntentIntegration(
                enable = enable,
                packageManager = packageManager,
                activityAliasName = ".SendAlias",
            )
            settingsRepository.enableShareIntegration(enable = enable)
        }
    }

    fun enableQuickSearch(enable: Boolean, packageManager: PackageManager) {
        viewModelScope.launch {
            enableIntentIntegration(
                enable = enable,
                packageManager = packageManager,
                activityAliasName = ".ProcessTextAlias",
            )
            settingsRepository.enableQuickSearch(enable = enable)
        }
    }

    private fun enableIntentIntegration(
        enable: Boolean,
        packageManager: PackageManager,
        activityAliasName: String,
    ) {
        val packageName = BuildConfig.APPLICATION_ID
        val componentName = ComponentName(packageName, "$packageName$activityAliasName")

        val componentEnabledState = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        packageManager.setComponentEnabledSetting(
            componentName,
            componentEnabledState,
            PackageManager.DONT_KILL_APP,
        )
    }

    fun exportLogs(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val printWriter = PrintWriter(outputStream.bufferedWriter(Charsets.UTF_8))

            val logcatProcess = Runtime.getRuntime().exec("logcat -d")
            val logsBufferedReader = logcatProcess.inputStream.bufferedReader(Charsets.UTF_8)

            printWriter.use { logsBufferedReader.forEachLine(it::println) }
            logcatProcess.destroy()
        }
    }
}