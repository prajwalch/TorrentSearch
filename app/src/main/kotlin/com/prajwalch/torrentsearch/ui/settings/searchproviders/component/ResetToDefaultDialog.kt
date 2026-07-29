package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R

@Composable
fun ResetToDefaultDialog(
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_reset_settings),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.search_providers_reset_to_default_title)) },
        text = { Text(stringResource(R.string.search_providers_reset_to_default_text)) },
        confirmButton = {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.search_providers_reset_to_default_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.search_providers_reset_to_default_button_dismiss))
            }
        },
    )
}