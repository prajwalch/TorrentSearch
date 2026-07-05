package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R

@Composable
fun CheckConnectionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCheckingConnection: Boolean = false,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
    ) {
        if (isCheckingConnection) {
            CircularProgressIndicator(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                strokeWidth = 2.0.dp,
            )
        } else {
            Icon(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                painter = painterResource(R.drawable.ic_network_check),
                contentDescription = null,
            )
        }

        Spacer(Modifier.width(ButtonDefaults.IconSpacing))

        val labelResId = if (isCheckingConnection) {
            R.string.search_providers_button_check_connection_checking
        } else {
            R.string.search_providers_button_check_connection
        }
        Text(stringResource(labelResId))
    }
}