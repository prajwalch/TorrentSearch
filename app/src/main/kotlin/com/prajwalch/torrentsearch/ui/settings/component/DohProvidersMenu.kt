package com.prajwalch.torrentsearch.ui.settings.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.DohProvider
import com.prajwalch.torrentsearch.ui.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DohProvidersMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedDohProvider: DohProvider,
    onDohProviderSelect: (DohProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text(stringResource(R.string.settings_dns_over_https_label_provider)) },
            value = selectedDohProvider.displayName(),
            onValueChange = {},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            readOnly = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        DohProvidersDropdownMenu(
            expanded = expanded,
            onDismiss = { onExpandedChange(false) },
            selectedDohProvider = selectedDohProvider,
            onDohProviderSelect = onDohProviderSelect,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.DohProvidersDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedDohProvider: DohProvider,
    onDohProviderSelect: (DohProvider) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 420.dp,
) {
    ExposedDropdownMenu(
        modifier = modifier.heightIn(max = maxHeight),
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DohProvider.entries.forEach { provider ->
            DropdownMenuItem(
                text = { Text(provider.displayName()) },
                onClick = { onDohProviderSelect(provider) },
                trailingIcon = {
                    if (provider == selectedDohProvider) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                        )
                    }
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
        }
    }
}