package com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

import com.prajwalch.torrentsearch.network.HttpClient

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
                onChallengeSolved = onChallengeSolved
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
    visibility = View.GONE

    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.userAgentString = HttpClient.USER_AGENT

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