package com.prajwalch.torrentsearch.ui.crash

import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constants.TorrentSearchConstants
import com.prajwalch.torrentsearch.ui.theme.spaces

private val CreateDocumentContract =
    ActivityResultContracts.CreateDocument(TorrentSearchConstants.CRASH_LOGS_FILE_TYPE)

@Composable
fun CrashScreen(
    stackTrace: String,
    onExportCrashLogsToFile: (Uri) -> Unit,
    onRestartApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exportLocationChooser = rememberLauncherForActivityResult(
        contract = CreateDocumentContract,
    ) { uri ->
        uri?.let(onExportCrashLogsToFile)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(MaterialTheme.spaces.large),
            verticalArrangement = Arrangement.spacedBy(space = MaterialTheme.spaces.large),
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(R.drawable.ic_bug_report),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
            CrashScreenTitle()
            CrashScreenSubtitle()
            StackTraceCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                stackTrace = stackTrace,
            )
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.small)) {
                ExportCrashLogsButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        exportLocationChooser.launch(TorrentSearchConstants.CRASH_LOGS_FILE_NAME)
                    },
                )
                RestartApplicationButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestartApp,
                )
            }
        }
    }
}

@Composable
private fun CrashScreenTitle(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.crash_screen_title),
        style = MaterialTheme.typography.headlineSmall,
    )
}

@Composable
private fun CrashScreenSubtitle(modifier: Modifier = Modifier) {
    val subtitle = stringResource(
        R.string.crash_screen_subtitle,
        stringResource(R.string.app_name),
    )
    Text(
        modifier = modifier,
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun StackTraceCard(stackTrace: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(all = MaterialTheme.spaces.large)
                .verticalScroll(state = rememberScrollState()),
        ) {
            Text(text = stackTrace)
        }
    }
}

@Composable
fun ExportCrashLogsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(modifier = modifier, onClick = onClick) {
        Text(text = stringResource(R.string.crash_button_export_crash_logs))
    }
}

@Composable
private fun RestartApplicationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(modifier = modifier, onClick = onClick) {
        Text(text = stringResource(R.string.crash_button_restart_application))
    }
}