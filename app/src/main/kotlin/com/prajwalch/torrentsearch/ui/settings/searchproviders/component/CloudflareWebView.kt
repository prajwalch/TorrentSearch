package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import com.prajwalch.torrentsearch.network.HttpClient

@Composable
fun BoxedCloudflareWebView(
    url: String,
    onChallengeSolved: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val boxAlpha by animateFloatAsState(
        targetValue = if (height == 0.dp) 0f else 1f,
        animationSpec = tween(200),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy))
            .height(height)
            .clip(shape = MaterialTheme.shapes.medium)
            .clipToBounds()
            .alpha(boxAlpha),
    ) {
        CloudflareWebView(
            modifier = Modifier.matchParentSize(),
            url = url,
            onChallengeSolved = onChallengeSolved,
        )
    }
}

@Composable
fun CloudflareWebView(
    url: String,
    onChallengeSolved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            createCloudflareWebView(
                context = context,
                onChallengeSolved = onChallengeSolved,
            ).apply { loadUrl(url) }
        },
        onRelease = {
            it.stopLoading()
            it.destroy()
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
fun createCloudflareWebView(
    context: Context,
    onChallengeSolved: () -> Unit,
): WebView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.userAgentString = HttpClient.USER_AGENT

    // Pair with theme.
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        settings.isAlgorithmicDarkeningAllowed = true
//    }

    webViewClient = CloudflareWebViewClient(onChallengeSolved)
}