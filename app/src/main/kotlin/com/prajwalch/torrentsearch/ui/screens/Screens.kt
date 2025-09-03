package com.prajwalch.torrentsearch.ui.screens

object Screens {
    const val SEARCH = "search"
    const val BOOKMARKS = "bookmarks"

    object Settings {
        const val ROOT = "settings"
        const val MAIN = "settings/main"
        const val DEFAULT_CATEGORY = "settings/default_category"
        const val DEFAULT_SORT_OPTIONS = "settings/default_sort_options"
        const val SEARCH_HISTORY = "settings/search_history"

        object SearchProviders {
            const val ROOT = "settings/search_providers"
            const val HOME = "settings/search_providers/home"
            const val ADD = "settings/search_providers/add"
            const val EDIT = "settings/search_providers/edit/{id}"

            fun createEditRoute(id: String): String {
                return EDIT.replace("{id}", id)
            }
        }
    }
}