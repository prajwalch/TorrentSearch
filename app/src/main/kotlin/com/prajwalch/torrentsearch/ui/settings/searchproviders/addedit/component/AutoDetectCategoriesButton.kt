package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.prajwalch.torrentsearch.R

@Composable
fun AutoDetectCategoriesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDetecting: Boolean = false,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        enabled = enabled,
    ) {
        if (!isDetecting) {
            Icon(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                painter = painterResource(R.drawable.ic_category_search),
                contentDescription = null,
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                strokeWidth = 2.0.dp,
            )
        }

        Spacer(Modifier.width(ButtonDefaults.IconSpacing))

        Crossfade(isDetecting) { isDetectingCategories ->
            val labelResId = if (isDetectingCategories) {
                R.string.search_providers_button_auto_detect_categories_detecting
            } else {
                R.string.search_providers_button_auto_detect_categories
            }
            Text(stringResource(labelResId))
        }
    }
}