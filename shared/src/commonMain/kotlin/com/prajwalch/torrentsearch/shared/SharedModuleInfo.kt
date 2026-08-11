package com.prajwalch.torrentsearch.shared

/**
 * Marker object for the shared Kotlin Multiplatform module.
 *
 * This module hosts portable (platform-agnostic) code such as domain models
 * and networking logic that can eventually be shared across Android and
 * other Kotlin Multiplatform targets (e.g. desktop, iOS). Code is expected
 * to be migrated here incrementally from the `:app` module.
 */
object SharedModuleInfo {
    const val NAME: String = "torrentsearch-shared"
}
