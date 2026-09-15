package com.prajwalch.torrentsearch.domain.model

/**
 * Specifies where the provider comes from.
 */
enum class SearchProviderOrigin {
    /**
     * Provider is bundled with the app.
     */
    Builtin,

    /**
     * Provider is added by the user and is Torznab compatible.
     */
    Torznab,
}