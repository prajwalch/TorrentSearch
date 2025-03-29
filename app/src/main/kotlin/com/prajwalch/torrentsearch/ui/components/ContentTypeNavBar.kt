package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.data.ContentType

@Composable
fun ContentTypeNavBar(activeContentType: ContentType, onSelect: (ContentType) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(ContentType.entries.toList()) { index, contentType ->
            // TODO: This is hack to add space between items.
            //       horizontalArrangement is not working.
            if (index != 0) {
                Spacer(Modifier.width(10.dp))
            }

            ContentTypeNavBarItem(
                label = contentType.toString(),
                isActive = activeContentType == contentType,
                onClick = { onSelect(contentType) }
            )
        }
    }
}

@Composable
private fun ContentTypeNavBarItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (isActive) Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Selected content type",
                modifier = Modifier.Companion.size(FilterChipDefaults.IconSize)
            )
        },
    )
}