package com.prajwalch.torrentsearch.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prajwalch.torrentsearch.R

@Composable
fun SettingsDialog(
    @StringRes
    title: Int,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.settings_dialog_button_cancel))
            }
        },
        confirmButton = {},
        title = { Text(text = stringResource(title)) },
        text = content,
    )
}