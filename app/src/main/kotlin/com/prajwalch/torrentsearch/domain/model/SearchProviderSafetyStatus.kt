package com.prajwalch.torrentsearch.domain.model

import androidx.annotation.StringRes

/**
 * Provider safety indication.
 */
sealed class SearchProviderSafetyStatus {
    /**
     * Provider is safe and trustworthy.
     */
    data object Safe : SearchProviderSafetyStatus()

    /**
     * Provider is not safe and should be used carefully.
     */
    data class Unsafe(@StringRes val reason: Int) : SearchProviderSafetyStatus()

    /**
     * Returns `true` if the status is [SearchProviderSafetyStatus.Unsafe].
     */
    fun isUnsafe(): Boolean = this is Unsafe
}