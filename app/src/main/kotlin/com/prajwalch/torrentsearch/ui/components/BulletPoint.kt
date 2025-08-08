package com.prajwalch.torrentsearch.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BulletPoint(modifier: Modifier = Modifier) {
    Text(modifier = modifier, text = "\u2022")
}