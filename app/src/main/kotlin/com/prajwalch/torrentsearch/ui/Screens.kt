package com.prajwalch.torrentsearch.ui

import com.prajwalch.torrentsearch.domain.models.Category

object Screens {
    const val HOME = "home"
    const val BOOKMARKS = "bookmarks"
    const val SEARCH = "search/{query}/{category}"
    const val SEARCH_HISTORY = "search_history"

    fun createSearchRoute(query: String, category: Category): String {
        return "search/$query/${category.name}"
    }

    object Settings {
        const val ROOT = "settings"
        const val MAIN = "settings/main"
        const val DEFAULT_CATEGORY = "settings/default_category"
        const val DEFAULT_SORT_OPTIONS = "settings/default_sort_options"

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