package com.prajwalch.torrentsearch.models

/** Dark theme options. */
enum class DarkTheme {
    On,
    Off,
    FollowSystem {
        override fun toString() = "Follow System"
    };
}

/** Results sort criteria. */
enum class SortCriteria {
    Name,
    Seeders,
    Peers,
    FileSize {
        override fun toString() = "File size"
    },
    Date;

    companion object {
        /** The default criteria. */
        val Default = Seeders
    }
}

/** Results sort order. */
enum class SortOrder {
    Ascending,
    Descending;

    /** Returns the opposite order. */
    fun opposite() = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }

    companion object {
        /** The default sort order. */
        val Default = Descending
    }
}

/** Defines maximum number of results to be shown. */
data class MaxNumResults(val n: Int) {
    fun isUnlimited() = n == UNLIMITED_N

    companion object {
        private const val UNLIMITED_N = -1

        val Unlimited = MaxNumResults(n = UNLIMITED_N)
    }
}