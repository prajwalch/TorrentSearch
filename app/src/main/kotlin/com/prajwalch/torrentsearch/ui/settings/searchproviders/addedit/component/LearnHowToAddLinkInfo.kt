package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.constant.TorrentSearchConstants
import com.prajwalch.torrentsearch.ui.component.BottomInfo
import com.prajwalch.torrentsearch.ui.component.TextUrl

@Composable
fun LearnHowToAddLinkInfo(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    BottomInfo(modifier = modifier) {
        TextUrl(
            text = stringResource(R.string.search_providers_learn_how_to_add),
            onClick = { uriHandler.openUri(TorrentSearchConstants.TORZNAB_HOW_TO_ADD_WIKI) },
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}