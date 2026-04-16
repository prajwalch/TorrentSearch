package com.prajwalch.torrentsearch.ui.main

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.DarkTheme
import com.prajwalch.torrentsearch.domain.model.MagnetUri
import com.prajwalch.torrentsearch.ui.TorrentSearchApp
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")

        installSplashScreen()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }

        val initialSearchQuery = getInitialSearchQuery()

        enableEdgeToEdge()
        setContent {
            val mainViewModel = hiltViewModel<MainViewModel>()
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = when (uiState.darkTheme) {
                DarkTheme.On -> true
                DarkTheme.Off -> false
                DarkTheme.FollowSystem -> isSystemInDarkTheme()
            }

            TorrentSearchTheme(
                darkTheme = darkTheme,
                dynamicColor = uiState.enableDynamicTheme,
                pureBlack = uiState.pureBlack,
            ) {
                Surface {
                    TorrentSearchApp(
                        onDownloadTorrent = ::downloadTorrentViaClient,
                        onShareMagnetLink = ::shareMagnetLink,
                        onShareDescriptionPageUrl = ::shareDescriptionPageUrl,
                        initialSearchQuery = initialSearchQuery,
                        openTorrentDetailsInApp = uiState.openTorrentDetailsInApp,
                    )
                }
            }
        }
    }

    /**
     * Returns the initial search query from the intent if the intent
     * is supported and contains a valid text.
     */
    private fun getInitialSearchQuery(): String? {
        Log.d(TAG, "getInitialSearchQuery [action = ${intent.action}, type = ${intent.type}]")

        val textReceivedFromIntent = when (intent.action) {
            Intent.ACTION_SEARCH -> intent.getStringExtra(SearchManager.QUERY)

            Intent.ACTION_SEND if (intent.type == PLAIN_TEXT_MIME_TYPE) -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }

            Intent.ACTION_PROCESS_TEXT if (intent.type == PLAIN_TEXT_MIME_TYPE) -> {
                intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            }

            else -> return null
        }

        if (textReceivedFromIntent == null) {
            Log.d(TAG, "Text not found in intent")
            return null
        }

        Log.d(TAG, "Received text '$textReceivedFromIntent'")

        if (textReceivedFromIntent.isBlank()) {
            val cannotSearchUsingBlankQueryMessage = getString(
                R.string.main_cannot_search_blank_query_message,
            )
            showToast(message = cannotSearchUsingBlankQueryMessage)

            return null
        }

        val urlPatternMatcher = Patterns.WEB_URL.matcher(textReceivedFromIntent)
        if (urlPatternMatcher.matches()) {
            val cannotSearchUsingUrlMessage = getString(
                R.string.main_cannot_search_using_url_message,
            )
            showToast(message = cannotSearchUsingUrlMessage)

            return null
        }

        val initialSearchQuery = urlPatternMatcher.replaceAll("").trim().trim('"', '\n')
        Log.d(TAG, "Initial search query is now '$initialSearchQuery'")

        return initialSearchQuery
    }

    /** Shows a toast with a given message. */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Starts the available torrent client for downloading the given torrent.
     *
     * @return `true` if the client starts successfully, `false` otherwise.
     */
    private fun downloadTorrentViaClient(magnetUri: MagnetUri): Boolean {
        Log.d(TAG, "downloadTorrentClientViaClient")

        return try {
            val torrentClientOpenIntent = Intent(Intent.ACTION_VIEW, magnetUri.toUri())
            startActivity(torrentClientOpenIntent)
            true
        } catch (_: ActivityNotFoundException) {
            Log.d(TAG, "Torrent client activity not found")
            false
        }
    }

    /** Starts the application chooser to share magnet uri with. */
    private fun shareMagnetLink(magnetUri: MagnetUri) {
        Log.d(TAG, "shareMagnetLink")

        try {
            startTextShareIntent(magnetUri)
        } catch (_: ActivityNotFoundException) {
            Log.d(TAG, "Activity not found")
        }
    }

    /** Starts the application chooser to share url with. */
    private fun shareDescriptionPageUrl(url: String) {
        Log.d(TAG, "shareDescriptionPage")

        try {
            startTextShareIntent(url)
        } catch (_: ActivityNotFoundException) {
            Log.d(TAG, "Activity not found")
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
        private const val PLAIN_TEXT_MIME_TYPE = "text/plain"
    }
}