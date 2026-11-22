package com.prajwalch.torrentsearch.ui.searchproviders.addedit.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType

import com.prajwalch.torrentsearch.R

@Composable
fun OutlinedUrlTextField(
    url: String,
    onUrlChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        value = url,
        onValueChange = onUrlChange,
        label = { Text(stringResource(R.string.search_providers_label_url)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_link),
                contentDescription = null,
            )
        },
        trailingIcon = if (isError) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                )
            }
        } else {
            null
        },
        supportingText = if (isError) {
            {
                Text(stringResource(R.string.search_providers_url_validation_error))
            }
        } else {
            null
        },
        isError = isError,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            errorLeadingIconColor = MaterialTheme.colorScheme.error,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
}