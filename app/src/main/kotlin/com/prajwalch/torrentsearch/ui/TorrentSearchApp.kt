package com.prajwalch.torrentsearch.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prajwalch.torrentsearch.models.MagnetUri
import com.prajwalch.torrentsearch.ui.components.TorrentClientNotFoundDialog
import com.prajwalch.torrentsearch.ui.screens.Screens
import com.prajwalch.torrentsearch.ui.screens.SearchScreen
import com.prajwalch.torrentsearch.ui.screens.settings.SettingsScreen
import com.prajwalch.torrentsearch.ui.viewmodel.SearchViewModel
import com.prajwalch.torrentsearch.ui.viewmodel.SettingsViewModel

@Composable
fun TorrentSearchApp(
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    onDownloadTorrent: (MagnetUri) -> Boolean,
    onShareMagnetLink: (MagnetUri) -> Unit,
    onOpenDescriptionPage: (String) -> Unit,
    onShareDescriptionPageUrl: (String) -> Unit,
) {
    val navController = rememberNavController()
    var isTorrentClientMissing by remember { mutableStateOf(false) }

    if (isTorrentClientMissing) {
        TorrentClientNotFoundDialog(
            onConfirmation = { isTorrentClientMissing = false },
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screens.HOME,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable(route = Screens.HOME) {
            SearchScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = searchViewModel,
                onNavigateToSettings = { navController.navigate(Screens.SETTINGS) },
                onDownloadTorrent = { isTorrentClientMissing = !onDownloadTorrent(it) },
                onShareMagnetLink = onShareMagnetLink,
                onOpenDescriptionPage = onOpenDescriptionPage,
                onShareDescriptionPageUrl = onShareDescriptionPageUrl,
            )
        }

        composable(
            route = Screens.SETTINGS,
            enterTransition = { slideInHorizontally() { fullWidth -> fullWidth } },
            exitTransition = { slideOutHorizontally() { fullWidth -> fullWidth } }
        ) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}