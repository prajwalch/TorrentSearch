package com.prajwalch.torrentsearch.domain.model

/**
 * Specifies from where the provider comes from.
 */
enum class SearchProviderType {
    /**
     * Provider is bundled with the app.
     */
    Builtin,

    /**
     * Provider is added by the user and is Torznab compatible.
     */
    Torznab
}