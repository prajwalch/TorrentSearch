package com.prajwalch.torrentsearch.ui.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun MaxNumResultsDialog(
    num: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onUnlimitedClick: () -> Unit,
    modifier: Modifier = Modifier,
    sliderRange: ClosedFloatingPointRange<Float> = 10f..100f,
    incrementBy: Int = 5,
) {
    val sliderState = rememberSliderState(
        value = num?.toFloat() ?: sliderRange.start,
        steps = ((sliderRange.endInclusive - sliderRange.start) / incrementBy).toInt() - 1,
        trackRange = sliderRange
    )

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_max_num_results)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_max_num_results_summary_format,
                        sliderState.value.toInt(),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spaces.large))
                Slider(
                    state = sliderState,
                    onValueChange = { sliderState.value = (it / incrementBy) * incrementBy },
                )
                OutlinedButton(onClick = onUnlimitedClick) {
                    Text(text = stringResource(R.string.settings_max_num_results_button_unlimited))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderState.value.toInt()) }) {
                Text(text = stringResource(R.string.button_done))
            }
        },
    )
}