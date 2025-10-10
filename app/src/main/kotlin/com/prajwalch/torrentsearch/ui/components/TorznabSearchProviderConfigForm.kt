package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.TorznabSearchProviderConfig
import com.prajwalch.torrentsearch.ui.theme.spaces
import com.prajwalch.torrentsearch.utils.categoryStringResource

@Composable
fun TorznabSearchProviderConfigForm(
    config: TorznabSearchProviderConfig,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onSafetyStatusChange: (SearchProviderSafetyStatus) -> Unit,
    isUrlValid: Boolean,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.spaces.small,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = config.name,
            onValueChange = onNameChange,
            label = { Text(text = stringResource(R.string.search_providers_label_name)) },
            singleLine = true,
        )
        OutlinedUrlTextField(
            modifier = Modifier.fillMaxWidth(),
            url = config.url,
            onUrlChange = onUrlChange,
            isError = !isUrlValid,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = config.apiKey,
            onValueChange = onApiKeyChange,
            label = { Text(text = stringResource(R.string.search_providers_label_api_key)) },
            singleLine = true,
        )

        Text(
            modifier = Modifier.padding(vertical = MaterialTheme.spaces.large),
            text = stringResource(R.string.search_providers_section_additional_options),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
        )

        OutlinedCategoryField(
            modifier = Modifier.fillMaxWidth(),
            value = config.category,
            onValueChange = onCategoryChange,
        )
        OutlinedSafetyStatusField(
            modifier = Modifier.fillMaxWidth(),
            value = config.safetyStatus,
            onValueChange = onSafetyStatusChange,
        )

        Box(
            modifier = Modifier.padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            confirmButton()
        }
    }
}

@Composable
private fun OutlinedUrlTextField(
    url: String,
    onUrlChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val textFieldColors = OutlinedTextFieldDefaults
        .colors(
            errorTextColor = MaterialTheme.colorScheme.error,
            errorSupportingTextColor = MaterialTheme.colorScheme.error,
        )

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
            Text(
                text = stringResource(R.string.search_providers_url_validation_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        null
    }

    OutlinedTextField(
        modifier = modifier,
        value = url,
        onValueChange = onUrlChange,
        label = { Text(text = stringResource(R.string.search_providers_label_url)) },
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        singleLine = true,
        colors = textFieldColors,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutlinedCategoryField(
    value: Category,
    onValueChange: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = modifier.menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
            ),
            value = categoryStringResource(value),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.search_providers_label_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            Category.entries.forEach {
                DropdownMenuItem(
                    text = { Text(text = categoryStringResource(it)) },
                    onClick = {
                        onValueChange(it)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutlinedSafetyStatusField(
    value: SearchProviderSafetyStatus,
    onValueChange: (SearchProviderSafetyStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeStatus = stringResource(R.string.safety_status_safe)
    val unsafeStatus = stringResource(R.string.safety_status_unsafe)

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = modifier.menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
            ),
            value = if (value.isUnsafe()) unsafeStatus else safeStatus,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.search_providers_label_safety_status)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            DropdownMenuItem(
                text = { Text(text = safeStatus) },
                onClick = {
                    onValueChange(SearchProviderSafetyStatus.Safe)
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
            DropdownMenuItem(
                text = { Text(text = unsafeStatus) },
                onClick = {
                    onValueChange(SearchProviderSafetyStatus.Unsafe(reason = ""))
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
        }
    }
}