package com.prajwalch.torrentsearch.ui

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
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.DarkTheme
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var startDestination = Screens.SEARCH

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate() called")

        installSplashScreen()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }
        handleIntent()

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
                TorrentSearchApp(
                    onDownloadTorrent = ::downloadTorrentViaClient,
                    onShareMagnetLink = ::shareMagnetLink,
                    onOpenDescriptionPage = ::openDescriptionPage,
                    onShareDescriptionPageUrl = ::shareDescriptionPageUrl,
                    startDestination = startDestination,
                )
            }
        }
    }

    /** Handles the intent that started this activity. */
    private fun handleIntent() {
        val intent = intent ?: return

        val action = intent.action
        val type = intent.type

        when (action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == type) {
                    handleSendText(intent)
                }
            }

            Intent.ACTION_PROCESS_TEXT -> {
                if ("text/plain" == type) {
                    handleProcessText(intent)
                }
            }

            else -> {}
        }
    }

    /**
     * Handles the text received from [Intent.ACTION_SEND] and updates the UI.
     */
    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Log.i(TAG, "Received '$it' from Intent.ACTION_SEND")
            performSearch(text = it)
        }
    }

    /**
     * Handles the text received from [Intent.ACTION_PROCESS_TEXT] and updates
     * the UI.
     */
    private fun handleProcessText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
            Log.i(TAG, "Received '$it' from Intent.ACTION_PROCESS_TEXT")
            performSearch(text = it)
        }
    }

    /** Sets the given text as the initial search query. */
    private fun performSearch(text: String) {
        val urlPatternMatcher = Patterns.WEB_URL.matcher(text)

        if (urlPatternMatcher.matches()) {
            Log.w(TAG, "Cannot perform search; text is a URL")

            Toast.makeText(this, "Cannot search using a URL", Toast.LENGTH_LONG).show()
            return
        }

        val text = urlPatternMatcher.replaceAll("").trim().trim('"', '\n')
        Log.d(TAG, "Performing search; query = $text")

        startDestination = Screens.createSearchResultsRoute(
            query = text,
            // FIXME: Default category is not respected.
            category = Category.All,
        )
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