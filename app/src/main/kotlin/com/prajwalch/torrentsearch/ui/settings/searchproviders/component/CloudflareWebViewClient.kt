package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import android.util.Log

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

import com.prajwalch.torrentsearch.network.HttpClient

class CloudflareWebViewClient(
    private val onChallengeSolved: () -> Unit,
) : WebViewClient() {
    private var challengeFound = false

    override fun onPageFinished(view: WebView?, url: String) {
        super.onPageFinished(view, url)
        Log.i(TAG, "Finished loading $url")

        if (!challengeFound) {
            Log.i(TAG, "Challenge not found; Aborting")
            onChallengeSolved()
            return
        }

        val challengeSolved = HttpClient.getCookie(url).let {
            it != null && it.contains("cf_clearance")
        }
        if (challengeSolved) {
            Log.i(TAG, "Found the cookie")
            onChallengeSolved()
        } else {
            Log.w(TAG, "Cookie not found")
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)

        val isErrorStatus = errorResponse?.statusCode in ERROR_STATUS_CODES
        val challengeHeader = errorResponse?.responseHeaders?.containsKey("cf_mitigated")

        if (request?.isForMainFrame == true &&
            (challengeHeader != null || isErrorStatus)
        ) {
            Log.i(TAG, "Found challenge page")
            challengeFound = true
        }
    }

    private companion object {
        private const val TAG = "CloudflareWebViewClient"
        private val ERROR_STATUS_CODES = listOf(403, 503)
    }
}