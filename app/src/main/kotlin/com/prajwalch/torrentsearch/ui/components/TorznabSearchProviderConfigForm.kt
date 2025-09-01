package com.prajwalch.torrentsearch.ui.components

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val urlPatternMatcher = Patterns.WEB_URL.matcher(config.url)
    var isUrlValid by rememberSaveable { mutableStateOf(true) }

    val enableSaveButton = remember(config) {
        config.name.isNotEmpty() && config.url.isNotEmpty() && config.apiKey.isNotEmpty()
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
    ) {
        item {
            OutlinedTextField(
                value = config.name,
                onValueChange = onNameChange,
                label = { Text(text = stringResource(R.string.label_name)) },
                singleLine = true,
            )
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                val textFieldColors = OutlinedTextFieldDefaults
                    .colors(errorTextColor = MaterialTheme.colorScheme.error)

                OutlinedTextField(
                    value = config.url,
                    onValueChange = onUrlChange,
                    label = { Text(text = stringResource(R.string.label_url)) },
                    trailingIcon = {
                        AnimatedVisibility(visible = !isUrlValid) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                            )
                        }
                    },
                    isError = !isUrlValid,
                    singleLine = true,
                    colors = textFieldColors,
                )
                AnimatedVisibility(visible = !isUrlValid) {
                    Text(
                        text = "Not a valid URL",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = config.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text(text = stringResource(R.string.label_api_key)) },
                singleLine = true,
            )
        }
        item {
            OutlinedCategoryField(
                value = config.category,
                onValueChange = onCategoryChange,
            )
        }
        item {
            OutlinedSafetyStatusField(
                value = config.safetyStatus,
                onValueChange = onSafetyStatusChange,
            )
        }
        item {
            Button(
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
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
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