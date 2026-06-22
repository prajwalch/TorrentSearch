package com.prajwalch.torrentsearch.ui.settings.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.DohProvider
import com.prajwalch.torrentsearch.ui.component.RoundedDropdownMenu
import com.prajwalch.torrentsearch.ui.displayName
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun DohProvidersDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selectedDohProvider: DohProvider,
    onDohProviderSelect: (DohProvider) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = 280.dp,
    maxHeight: Dp = 420.dp,
) {
    RoundedDropdownMenu(
        modifier = Modifier
            .widthIn(min = minWidth)
            .heightIn(max = maxHeight)
            .then(modifier),
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 52.dp, y = 0.dp),
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
                contentPadding = PaddingValues(horizontal = MaterialTheme.spaces.large),
            )
        }
    }
}