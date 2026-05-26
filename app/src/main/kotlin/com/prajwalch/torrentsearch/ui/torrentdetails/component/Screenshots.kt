package com.prajwalch.torrentsearch.ui.torrentdetails.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.ui.theme.spaces

@Composable
fun ScreenShots(
    urls: List<String>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    DetailsSection(
        modifier = modifier,
        title = {
            Text(
                modifier = Modifier.padding(horizontal = MaterialTheme.spaces.large),
                text = stringResource(R.string.torrent_details_title_screenshots),
            )
        },
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spaces.large),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = contentPadding,
        ) {
            items(items = urls, key = { it }) {
                Screenshot(modifier = Modifier.animateItem(), url = it)
            }
        }
    }
}

@Composable
private fun Screenshot(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .height(180.dp)
            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
            .then(modifier),
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}