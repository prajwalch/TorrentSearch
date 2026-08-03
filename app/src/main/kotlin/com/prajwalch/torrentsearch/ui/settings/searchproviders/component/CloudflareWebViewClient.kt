package com.prajwalch.torrentsearch.ui.settings.searchproviders.component

import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

import com.prajwalch.torrentsearch.network.NetworkClient

sealed interface ChallengeSolveError {
    data object BadUrl : ChallengeSolveError
    data object ConnectFailed : ChallengeSolveError
    data object HostLookupFailed : ChallengeSolveError
    data object Timeout : ChallengeSolveError
    data object TooManyRedirects : ChallengeSolveError
    data object Unknown : ChallengeSolveError

    data class ApplicationError(val errorCode: Int) : ChallengeSolveError
}

class CloudflareWebViewClient(
    private val onChallengeSolved: () -> Unit,
    private val onError: (ChallengeSolveError) -> Unit,
) : WebViewClient() {
    private var challengeFound = false
    private var solveError: ChallengeSolveError? = null

    override fun onPageFinished(view: WebView?, url: String) {
        super.onPageFinished(view, url)
        Log.i(TAG, "Finished loading $url")

        val solveError = solveError
        if (solveError != null) {
            Log.e(TAG, "Error solving challenge $solveError")
            onError(solveError)
            return
        }

        if (!challengeFound) {
            Log.i(TAG, "Challenge not found")
            onChallengeSolved()
            return
        }

        val challengeSolved = NetworkClient.getCookie(url).let {
            it != null && it.contains("cf_clearance")
        }
        if (challengeSolved) {
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
            Log.e(TAG, "Received error")
            solveError = errorCodeToChallengeSolveError(error?.errorCode)
        }
    }

    private fun errorCodeToChallengeSolveError(errorCode: Int?): ChallengeSolveError {
        return when (errorCode) {
            null -> ChallengeSolveError.Unknown
            ERROR_BAD_URL -> ChallengeSolveError.BadUrl
            ERROR_CONNECT -> ChallengeSolveError.ConnectFailed
            ERROR_HOST_LOOKUP -> ChallengeSolveError.HostLookupFailed
            ERROR_REDIRECT_LOOP -> ChallengeSolveError.TooManyRedirects
            ERROR_TIMEOUT -> ChallengeSolveError.Timeout
            else -> ChallengeSolveError.ApplicationError(errorCode)
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