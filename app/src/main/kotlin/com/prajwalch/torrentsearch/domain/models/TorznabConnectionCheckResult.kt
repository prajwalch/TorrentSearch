package com.prajwalch.torrentsearch.domain.models

/** Result of the Torznab connection check. */
sealed class TorznabConnectionCheckResult {
    /** A successful connection established. */
    data object ConnectionEstablished : TorznabConnectionCheckResult()

    /** Failed to establish successful connection. */
    data object ConnectionFailed : TorznabConnectionCheckResult()

    /** API key is not valid. */
    data object InvalidApiKey : TorznabConnectionCheckResult()

    /** An internal application error occurred. */
    data class ApplicationError(val errorCode: Int) : TorznabConnectionCheckResult()

    /** Received an unexpected response. */
    data class UnexpectedResponse(val errorCode: Int) : TorznabConnectionCheckResult()

    /** An unexpected error occurred. */
    data object UnexpectedError : TorznabConnectionCheckResult()
}