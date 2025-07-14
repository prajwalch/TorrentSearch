package com.prajwalch.torrentsearch.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.prajwalch.torrentsearch.R

@Composable
fun NSFWTag(modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.nsfw),
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Black,
        style = style,
    )
}