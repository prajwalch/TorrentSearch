package com.prajwalch.torrentsearch.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

import com.prajwalch.torrentsearch.ui.tv.theme.spaces
import com.prajwalch.torrentsearch.ui.tv.theme.tvCardSurface

/**
 * Radio-style option picker.
 *
 * Replaces `DropdownMenu`, which on a remote has no focusable trigger and no way
 * to keep the selection anchored while focus is inside it. The settings screen
 * uses it for multi-value settings; the search screen uses it for sort criteria
 * and sort order.
 */
@Composable
fun TvOptionDialog(
    title: String,
    // label, whether this is the value currently in effect, and the action to apply it.
    options: List<Triple<String, Boolean, () -> Unit>>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.tv.material3.Surface(
            shape = MaterialTheme.shapes.extraLarge,
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.tvCardSurface,
            ),
            modifier = Modifier.fillMaxWidth(0.55f).padding(vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(MaterialTheme.spaces.large)
                    .padding(TvFocusDefaults.Reserve),
                verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // A bounded max height, not weight(): a Dialog measures its content
                // with unbounded height, so weight(1f, fill = false) collapses and the
                // last option gets clipped out of the panel.
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    // The list clips to its own bounds, so the first and last option
                    // need room for the scaled focus body and ring.
                    contentPadding = PaddingValues(vertical = TvFocusDefaults.Reserve),
                    verticalArrangement = Arrangement.spacedBy(TvFocusDefaults.Reserve),
                ) {
                    items(options.size) { index ->
                        val (label, isActive, onPick) = options[index]
                        TvMenuItem(
                            label = label,
                            // Mark the value already in effect, otherwise the dialog
                            // is just a list of words with no hint what is set.
                            active = isActive,
                            onClick = onPick,
                        )
                    }
                }
            }
        }
    }
}
