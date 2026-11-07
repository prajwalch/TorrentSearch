package com.prajwalch.torrentsearch.ui.crash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.util.LogsUtils
import com.prajwalch.torrentsearch.util.TorrentSearchExceptionHandler

import kotlinx.coroutines.launch

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stackTrace = TorrentSearchExceptionHandler.getCrashStackTrace(intent)

        enableEdgeToEdge()
        setContent {
            TorrentSearchTheme {
                CrashScreen(
                    stackTrace = stackTrace,
                    onExportCrashLogsToFile = { fileUri ->
                        stackTrace?.let { exportCrashLogsToFile(it, fileUri) }
                    },
                    onRestartApp = ::restartApplication,
                )
            }
        }
    }

    private fun exportCrashLogsToFile(stackTrace: String, fileUri: Uri) {
        lifecycleScope.launch {
            val outputStream = contentResolver.openOutputStream(fileUri) ?: return@launch
            LogsUtils.exportLogsToOutputStream(
                outputStream = outputStream,
                stackTrace = stackTrace,
            )
        }

        val successMessage = getString(R.string.crash_logs_export_success_message)
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
    }

    private fun restartApplication() {
        // Resolves against whichever launcher activity the active variant declares
        // (LAUNCHER on handheld, LEANBACK_LAUNCHER on TV) instead of hard-coding a
        // class name, so this stays correct across the device product flavors.
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        val restartIntent = Intent.makeRestartActivityTask(launchIntent.component)

        startActivity(restartIntent)
        finishAffinity()
    }
}