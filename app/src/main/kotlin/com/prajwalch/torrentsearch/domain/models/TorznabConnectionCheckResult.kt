package com.prajwalch.torrentsearch.domain.models

sealed class TorznabConnectionCheckResult {
    /** Connection check succeed. */
    data object Ok : TorznabConnectionCheckResult()

    /** Cannot connect to client. */
    data object CannotConnect : TorznabConnectionCheckResult()

    /** API key is not valid. */
    data object InvalidApiKey : TorznabConnectionCheckResult()

    /** API is disabled. */
    data object ApiDisabled : TorznabConnectionCheckResult()

    /** Internal application error occurred. */
    data class InternalApplicationError(val errorCode: Int) : TorznabConnectionCheckResult()

    /** Some other unknown error occurred. */
    data object UnknownError : TorznabConnectionCheckResult()
}