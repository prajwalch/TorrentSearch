package com.prajwalch.torrentsearch.domain.model

/**
 * Status of the protection.
 */
enum class CloudflareProtectionStatus {
    /**
     * Not protected at all.
     */
    UnProtected,

    /**
     * Protected but not unlocked yet.
     */
    Locked,

    /**
     * Protected but already unlocked.
     */
    Unlocked,
}