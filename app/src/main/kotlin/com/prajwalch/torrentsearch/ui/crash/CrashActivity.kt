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
import com.prajwalch.torrentsearch.ui.main.MainActivity
import com.prajwalch.torrentsearch.ui.theme.TorrentSearchTheme
import com.prajwalch.torrentsearch.utils.TorrentSearchExceptionHandler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stackTrace = TorrentSearchExceptionHandler.getCrashStackTrace(intent = intent)

        enableEdgeToEdge()
        setContent {
            TorrentSearchTheme {
                CrashScreen(
                    stackTrace = stackTrace,
                    onExportCrashLogsToFile = { exportCrashLogsToFile(stackTrace, it) },
                    onRestartApp = ::restartApplication,
                )
            }
        }
    }

    private fun exportCrashLogsToFile(stackTrace: String, fileUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val outputStream = contentResolver.openOutputStream(fileUri) ?: return@launch
            outputStream.use { it.write(stackTrace.toByteArray()) }
        }

        val successMessage = getString(R.string.crash_logs_export_success_message)
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
    }

    private fun restartApplication() {
        finishAffinity()

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        startActivity(mainActivityIntent)
    }

    private companion object {
        private const val TAG = "CrashActivity"
    }
}