package com.prajwalch.torrentsearch.ui.tv

import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.prajwalch.torrentsearch.domain.model.DarkTheme
import com.prajwalch.torrentsearch.ui.main.MainViewModel
import com.prajwalch.torrentsearch.ui.tv.theme.TvTheme

import org.koin.androidx.compose.koinViewModel

/**
 * The Android TV / Google TV entry point.
 *
 * Deliberately much smaller than the handheld `MainActivity`, which is phone
 * specific and lives in the `mobile` source set. Removed on TV:
 *
 * - `installSplashScreen()` / `enableEdgeToEdge()`: TV owns its own cold-start
 *   appearance and safe-area model.
 * - `ACTION_SEARCH` / `ACTION_SEND` / `ACTION_PROCESS_TEXT` intake: global
 *   search, share targets and text-selection processing have no TV analogue.
 *   Google TV's launcher owns search.
 * - The unconditional `moveTaskToBack` back callback: BACK must unwind the
 *   navigation stack first and only then reach the TV home screen. That is
 *   handled by `TvBackHandler` inside the nav graph.
 */
class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContent {
            TvApp()
        }
    }

    private companion object {
        private const val TAG = "TvMainActivity"
    }
}

@Composable
private fun TvApp() {
    val mainViewModel = koinViewModel<MainViewModel>()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val darkTheme = when (uiState.darkTheme) {
        DarkTheme.On -> true
        DarkTheme.Off -> false
        DarkTheme.FollowSystem -> isSystemInDarkTheme()
    }

    TvTheme(
        darkTheme = darkTheme,
        dynamicColor = uiState.enableDynamicTheme,
        pureBlack = uiState.pureBlack,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TorrentSearchTvApp()
        }
    }
}