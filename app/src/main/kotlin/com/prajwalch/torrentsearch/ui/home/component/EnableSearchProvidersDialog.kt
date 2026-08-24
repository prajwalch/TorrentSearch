package com.prajwalch.torrentsearch.ui.home.component

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
fun EnableSearchProvidersDialog(
    onDismiss: () -> Unit,
    onEnableRecommended: () -> Unit,
    onLetUserChoose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_hub),
                contentDescription = null,
            )
        },
        title = { Text(stringResource(R.string.home_title_enable_search_providers)) },
        text = { Text(stringResource(R.string.home_search_providers_not_enabled_msg)) },
        confirmButton = {
            TextButton(onClick = onEnableRecommended) {
                Text(stringResource(R.string.home_button_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onLetUserChoose) {
                Text(stringResource(R.string.home_button_i_will_choose_myself))
            }
        },
    )
}