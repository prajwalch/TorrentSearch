package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component

import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R

@Composable
fun OutlinedUrlTextField(
    url: String,
    onUrlChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val trailingIcon = if (isError) {
        @Composable {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
            )
        }
    } else {
        null
    }

    val supportingText = if (isError) {
        @Composable {
            Text(stringResource(R.string.search_providers_url_validation_error))
        }
    } else {
        null
    }

    OutlinedTextField(
        modifier = modifier,
        value = url,
        onValueChange = onUrlChange,
        label = { Text(stringResource(R.string.search_providers_label_url)) },
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        singleLine = true,
    )
}