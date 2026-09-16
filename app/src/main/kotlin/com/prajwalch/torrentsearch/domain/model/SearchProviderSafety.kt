package com.prajwalch.torrentsearch.domain.model

import androidx.annotation.StringRes

/**
 * Provider safety indication.
 */
sealed interface SearchProviderSafety {
    /**
     * Provider is safe and trustworthy.
     */
    data object Safe : SearchProviderSafety

    /**
     * Provider is not safe and should be used carefully.
     */
    data class Unsafe(@StringRes val reason: Int) : SearchProviderSafety
}

/**
 * Returns `true` if the status is [SearchProviderSafety.Unsafe].
 */
fun SearchProviderSafety.isUnsafe(): Boolean = this is SearchProviderSafety.Unsafe