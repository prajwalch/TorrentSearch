package com.prajwalch.torrentsearch.ui.components

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.TorznabSearchProviderConfig

@Composable
fun TorznabSearchProviderConfigForm(
    config: TorznabSearchProviderConfig,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onSafetyStatusChange: (SearchProviderSafetyStatus) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val urlPatternMatcher = Patterns.WEB_URL.matcher(config.url)
    var isUrlValid by rememberSaveable { mutableStateOf(true) }

    val enableSaveButton = remember(config) {
        config.name.isNotEmpty() && config.url.isNotEmpty() && config.apiKey.isNotEmpty()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(state = scrollState)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = config.name,
            onValueChange = onNameChange,
            label = { Text(text = stringResource(R.string.label_name)) },
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
            label = { Text(text = stringResource(R.string.label_api_key)) },
            singleLine = true,
        )

        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = stringResource(R.string.additional_options),
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

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            enabled = enableSaveButton,
            onClick = {
                if (urlPatternMatcher.matches()) {
                    onSave()
                } else if (isUrlValid) {
                    isUrlValid = false
                }
            },
        ) {
            Text(text = stringResource(R.string.button_save))
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
                imageVector = Icons.Default.Info,
                contentDescription = null,
            )
        }
    } else {
        null
    }

    val supportingText = if (isError) {
        @Composable {
            Text(
                text = stringResource(R.string.error_not_a_valid_url),
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
        label = { Text(text = stringResource(R.string.label_url)) },
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
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = value.name,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.label_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Category.entries.forEach {
                DropdownMenuItem(
                    text = { Text(text = it.name) },
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
    val values = listOf("Safe", "Unsafe")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = if (value.isUnsafe()) "Unsafe" else "Safe",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.label_safety_status)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            values.forEach {
                DropdownMenuItem(
                    text = { Text(text = it) },
                    onClick = {
                        val safetyStatus = if (it == "Safe") {
                            SearchProviderSafetyStatus.Safe
                        } else {
                            SearchProviderSafetyStatus.Unsafe(reason = "")
                        }
                        onValueChange(safetyStatus)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}