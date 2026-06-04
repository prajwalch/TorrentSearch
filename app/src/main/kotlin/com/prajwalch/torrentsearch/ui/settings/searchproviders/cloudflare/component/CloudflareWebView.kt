package com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

import com.prajwalch.torrentsearch.network.HttpClient

@Composable
fun BoxedCloudflareWebView(
    url: String,
    onChallengeSolved: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape = MaterialTheme.shapes.medium)
            .border(
                width = Dp.Hairline,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium,
            )
            .clipToBounds(),
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
            createWebView(
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
private fun createWebView(
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

private class CloudflareWebViewClient(
    private val onChallengeSolved: () -> Unit,
) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        if (url == null) return
        Log.i(TAG, "Finished loading $url")
        Log.i(TAG, "Checking Cloudflare clearance cookie for $url")

        val cookie = HttpClient.getCookie(url)
        if (cookie != null && cookie.contains("cf_clearance")) {
            Log.i(TAG, "Found the cookie")
            onChallengeSolved()
        } else {
            Log.w(TAG, "Cookie not found")
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)

        if (request?.isForMainFrame == true) {
            Log.e(TAG, "Received error while loading ${request.url}: ${error?.description}")
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)

        if (request?.isForMainFrame == true) {
            val url = request.url
            val statusCode = errorResponse?.statusCode
            val reason = errorResponse?.reasonPhrase

            Log.e(
                TAG,
                "Received HTTP error while loading $url ($statusCode): $reason",
            )
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {
        super.shouldOverrideUrlLoading(view, request)
        // Follow redirects.
        return false
    }

    private companion object {
        private const val TAG = "CloudflareWebViewClient"
    }
}