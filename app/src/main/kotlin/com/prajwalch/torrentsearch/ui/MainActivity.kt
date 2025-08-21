package com.prajwalch.torrentsearch.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.data.database.TorrentSearchDatabase
import com.prajwalch.torrentsearch.data.repository.DarkTheme
import com.prajwalch.torrentsearch.data.repository.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorrentsRepository
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.ui.viewmodel.BookmarksViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SearchHistoryViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SearchProvidersViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

class MainActivity : ComponentActivity() {
    private val database: TorrentSearchDatabase by lazy {
        TorrentSearchDatabase.getInstance(this)
    }

    // Repositories.

    private val searchHistoryRepository by lazy {
        SearchHistoryRepository(dao = database.searchHistoryDao())
    }

    private val searchProvidersRepository: SearchProvidersRepository by lazy {
        SearchProvidersRepository(dao = database.torznabSearchProviderDao())
    }

    private val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            dataStore = settingsDataStore,
            searchProvidersRepository = searchProvidersRepository,
        )
    }

    private val torrentsRepository: TorrentsRepository by lazy {
        TorrentsRepository(
            httpClient = HttpClient,
            bookmarkedTorrentDao = database.bookmarkedTorrentDao(),
        )
    }

    // ViewModels.

    private val bookmarksViewModel: BookmarksViewModel by viewModels {
        BookmarksViewModel.provideFactory(
            settingsRepository = settingsRepository,
            torrentsRepository = torrentsRepository
        )
    }

    private val searchHistoryViewModel: SearchHistoryViewModel by viewModels {
        SearchHistoryViewModel.providerFactory(searchHistoryRepository = searchHistoryRepository)
    }

    private val searchProvidersViewModel: SearchProvidersViewModel by viewModels {
        SearchProvidersViewModel.provideFactory(
            settingsRepository = settingsRepository,
            searchProvidersRepository = searchProvidersRepository,
        )
    }

    private val searchViewModel: SearchViewModel by viewModels {
        SearchViewModel.provideFactory(
            settingsRepository = settingsRepository,
            searchHistoryRepository = searchHistoryRepository,
            torrentsRepository = torrentsRepository,
            searchProvidersRepository = searchProvidersRepository,
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.provideFactory(
            settingsRepository = settingsRepository,
            searchProvidersRepository = searchProvidersRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate called")

        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }

        enableEdgeToEdge()
        setContent {
            val appearanceSettings by settingsViewModel.appearanceSettingsUiState.collectAsStateWithLifecycle()

            val darkTheme = when (appearanceSettings.darkTheme) {
                DarkTheme.On -> true
                DarkTheme.Off -> false
                DarkTheme.FollowSystem -> isSystemInDarkTheme()
            }

            TorrentSearchTheme(
                darkTheme = darkTheme,
                dynamicColor = appearanceSettings.enableDynamicTheme,
                pureBlack = appearanceSettings.pureBlack,
            ) {
                TorrentSearchApp(
                    bookmarksViewModel = bookmarksViewModel,
                    searchHistoryViewModel = searchHistoryViewModel,
                    searchProvidersViewModel = searchProvidersViewModel,
                    searchViewModel = searchViewModel,
                    settingsViewModel = settingsViewModel,
                    onDownloadTorrent = ::downloadTorrentViaClient,
                    onShareMagnetLink = ::shareMagnetLink,
                    onOpenDescriptionPage = ::openDescriptionPage,
                    onShareDescriptionPageUrl = ::shareDescriptionPageUrl,
                )
            }
        }
    }

    /**
     * Starts the available torrent client for downloading the given torrent.
     *
     * @return `true` if the client starts successfully, `false` otherwise.
     */
    private fun downloadTorrentViaClient(magnetUri: MagnetUri): Boolean {
        val torrentClientOpenIntent = Intent(Intent.ACTION_VIEW, magnetUri.toUri())

        return try {
            startActivity(torrentClientOpenIntent)
            true
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "Torrent client launch failed. (Activity not found)")
            false
        }
    }

    /** Starts the application chooser to share magnet uri with. */
    private fun shareMagnetLink(magnetUri: MagnetUri) {
        Log.d(TAG, "Sharing magnet URI: $magnetUri")
        try {
            startTextShareIntent(magnetUri)
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "Magnet uri share intent launch failed. (Activity not found)")
        }
    }

    /** Opens a description page in a default browser. */
    private fun openDescriptionPage(url: String) {
        val openPageIntent = Intent(Intent.ACTION_VIEW, url.toUri())

        try {
            startActivity(openPageIntent)
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "Failed to open description page. (Activity not found)")
        }
    }

    /** Starts the application chooser to share url with. */
    private fun shareDescriptionPageUrl(url: String) {
        try {
            startTextShareIntent(url)
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "Description page URL share intent launch failed. (Activity not found)")
        }
    }

    /** Starts the application chooser to share the text with. */
    private fun startTextShareIntent(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"

            putExtra(Intent.EXTRA_TEXT, text)
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private companion object {
        private const val TAG = "MainActivity"
    }
}