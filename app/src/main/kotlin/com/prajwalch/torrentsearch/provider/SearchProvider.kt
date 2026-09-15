package com.prajwalch.torrentsearch.provider

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SearchProviderOrigin
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.Torrent

/**
 * Represent the type of provider ID.
 */
typealias SearchProviderId = String

/**
 * A search provider is responsible for searching torrents.
 *
 * This is the core interface which contains the metadata of the provider
 * and a single required [search] function.
 */
interface SearchProvider {
    /**
     * Unique ID of the provider.
     *
     * An ID must remain stable across different app versions.
     */
    val id: SearchProviderId

    /**
     * Display name of the provider.
     */
    val name: String

    /**
     * Homepage URL of the provider.
     */
    val url: String

    /**
     * The URL which should be used for solving Cloudflare challenge.
     *
     * Some providers don't challenge the page until we request a specific URL.
     * If that's the case, set the URL here otherwise leave it to `null`
     * to indicate that the main [url] can be used.
     *
     * This value will be ignored if [isCloudflareProtected] is set to `false`.
     */
    val cloudflareSolverUrl: String? get() = null

    /**
     * Set of categories supported by the provider.
     *
     * Include a category if the upstream server either accepts from a search
     * parameter, or categorizes the returned results without supporting
     * category-based search. If neither, assign a set with a single
     * [Category.Other].
     */
    val supportedCategories: Set<Category> get() = emptySet()

    /**
     * Safety flag of the provider.
     *
     * Providers marked with [SearchProviderSafety.Unsafe] will
     * automatically be disabled when disabling NSFW mode and a special
     * badge will be shown on the UI to warn users.
     */
    val safety: SearchProviderSafety

    /**
     * Indicates where the provider comes from.
     */
    val origin: SearchProviderOrigin get() = SearchProviderOrigin.Builtin

    /**
     * Indicates whether the provider is Cloudflare-protected.
     */
    val isCloudflareProtected: Boolean get() = false

    /**
     * Indicates whether the provider should be enabled when initializing
     * providers for the very first time.
     */
    val enabledByDefault: Boolean

    /**
     * Searches torrents for a given query and category.
     *
     * Any exceptions thrown from this function will be caught safely.
     */
    suspend fun search(query: String, category: Category): List<Torrent>
}