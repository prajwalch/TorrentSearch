package com.prajwalch.torrentsearch.ui.settings

import android.content.ComponentName
import android.content.pm.PackageManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.BuildConfig
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.ViewedTorrentRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.DarkTheme
import com.prajwalch.torrentsearch.domain.model.DohProvider
import com.prajwalch.torrentsearch.domain.model.MaxNumResults
import com.prajwalch.torrentsearch.domain.model.SortOptions
import com.prajwalch.torrentsearch.util.LogsUtils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import org.koin.core.annotation.KoinViewModel
import java.io.OutputStream
import kotlin.time.Duration.Companion.seconds

data class SettingsUiState(
    val appearanceSettings: AppearanceSettingsUiState = AppearanceSettingsUiState(),
    val generalSettings: GeneralSettingsUiState = GeneralSettingsUiState(),
    val contentAndPrivacySettings: ContentAndPrivacySettingsUiState = ContentAndPrivacySettingsUiState(),
    val searchSettings: SearchSettingsUiState = SearchSettingsUiState(),
    val networkSettings: NetworkSettingsUiState = NetworkSettingsUiState(),
)

data class AppearanceSettingsUiState(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val pureBlack: Boolean = false,
)

data class GeneralSettingsUiState(
    val openTorrentDetailsInApp: Boolean = true,
    val enableShareIntegration: Boolean = true,
    val enableQuickSearch: Boolean = true,
)

data class ContentAndPrivacySettingsUiState(
    val enableNSFWMode: Boolean = false,
    val blurNSFWImages: Boolean = true,
    val saveSearchHistory: Boolean = true,
    val showSearchHistory: Boolean = true,
)

data class SearchSettingsUiState(
    val searchProvidersStat: SearchProvidersStat = SearchProvidersStat(),
    val defaultSortOptions: SortOptions = SortOptions(),
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
) {
    data class SearchProvidersStat(
        val enabledSearchProvidersCount: Int = 0,
        val totalSearchProvidersCount: Int = 0,
    )
}

data class NetworkSettingsUiState(
    val dohProvider: DohProvider = DohProvider.Default,
)

/** ViewModel that handles the business logic of Settings screen. */
@KoinViewModel
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchProvidersManager: SearchProvidersManager,
    private val viewedTorrentRepository: ViewedTorrentRepository,
) : ViewModel() {
    val uiState = combine(
        settingsRepository.getAppearanceSettings(),
        settingsRepository.getGeneralSettings(),
        settingsRepository.getContentAndPrivacySettings(),
        settingsRepository.getSearchSettings(searchProvidersManager.getProvidersCount()),
        settingsRepository.getNetworkSettings(),
        ::SettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SettingsUiState(),
    )

    fun enableDynamicTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableDynamicTheme(enable = enable)
        }
    }

    fun setDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(darkTheme = darkTheme)
        }
    }

    fun enablePureBlackTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enablePureBlack(enable = enable)
        }
    }

    fun enableNSFWMode(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableNSFWMode(enable = enable)
            if (!enable) searchProvidersManager.disableNsfwAndUnsafeProviders()
        }
    }

    fun enableBlurNSFWImages(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableBlurNSFWImages(enable)
        }
    }

    fun clearViewedTorrents() {
        viewModelScope.launch {
            viewedTorrentRepository.clearAll()
        }
    }

    fun enableSaveSearchHistory(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableSaveSearchHistory(enable = save)
        }
    }

    fun enableShowSearchHistory(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableShowSearchHistory(show = show)
        }
    }

    fun setMaxNumResults(maxNumResults: MaxNumResults) {
        viewModelScope.launch {
            settingsRepository.setMaxNumResults(maxNumResults = maxNumResults)
        }
    }

    fun enableOpenTorrentDetailsInApp(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableOpenTorrentDetailsInApp(enable)
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

    fun setDohProvider(provider: DohProvider) {
        viewModelScope.launch {
            settingsRepository.setDohProvider(provider)
        }
    }

    fun exportLogs(outputStream: OutputStream) {
        viewModelScope.launch {
            LogsUtils.exportLogsToOutputStream(outputStream = outputStream)
        }
    }
}

private fun SettingsRepository.getAppearanceSettings() =
    combine(
        this.enableDynamicTheme,
        this.darkTheme,
        this.pureBlack,
        ::AppearanceSettingsUiState,
    )

private fun SettingsRepository.getGeneralSettings() =
    combine(
        this.openTorrentDetailsInApp,
        this.enableShareIntegration,
        this.enableQuickSearch,
        ::GeneralSettingsUiState,
    )

private fun SettingsRepository.getContentAndPrivacySettings() =
    combine(
        this.enableNSFWMode,
        this.blurNSFWImages,
        this.saveSearchHistory,
        this.showSearchHistory,
        ::ContentAndPrivacySettingsUiState,
    )

private fun SettingsRepository.getSearchSettings(
    searchProvidersCount: Flow<Int>,
): Flow<SearchSettingsUiState> {
    val searchProvidersStat = combine(
        this.enabledSearchProviderIds.map { it?.size ?: 0 },
        searchProvidersCount,
        SearchSettingsUiState::SearchProvidersStat,
    )

    return combine(
        searchProvidersStat,
        this.defaultSortOptions,
        this.maxNumResults,
        ::SearchSettingsUiState,
    )
}

private fun SettingsRepository.getNetworkSettings() =
    this.dohProvider.map(::NetworkSettingsUiState)