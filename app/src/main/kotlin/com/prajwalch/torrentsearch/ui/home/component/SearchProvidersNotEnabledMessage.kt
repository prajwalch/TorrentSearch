package com.prajwalch.torrentsearch.ui.home.component

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.component.MessageCard
import com.prajwalch.torrentsearch.ui.component.MessageType

@Composable
fun SearchProvidersNotEnabledMessage(
    onEnableRecommended: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageCard(
        modifier = modifier,
        onClose = onClose,
        messageType = MessageType.Info,
        text = {
            Text(stringResource(R.string.home_search_providers_not_enabled_msg))
        },
        confirmButton = {
            TextButton(onClick = onEnableRecommended) {
                Text(stringResource(R.string.home_button_enable_recommended))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.home_button_i_will_choose_myself))
            }
        },
    )
}