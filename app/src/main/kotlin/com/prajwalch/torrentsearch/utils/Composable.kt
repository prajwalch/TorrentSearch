package com.prajwalch.torrentsearch.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.SortCriteria
import com.prajwalch.torrentsearch.models.SortOrder

@Composable
fun categoryStringResource(category: Category): String {
    val resId = when (category) {
        Category.All -> R.string.category_all
        Category.Anime -> R.string.category_anime
        Category.Apps -> R.string.category_apps
        Category.Books -> R.string.category_books
        Category.Games -> R.string.category_games
        Category.Movies -> R.string.category_movies
        Category.Music -> R.string.category_music
        Category.Porn -> R.string.category_porn
        Category.Series -> R.string.category_series
        Category.Other -> R.string.category_other
    }

    return stringResource(id = resId)
}

@Composable
fun sortCriteriaStringResource(sortCriteria: SortCriteria): String {
    val resId = when (sortCriteria) {
        SortCriteria.Name -> R.string.action_sort_criteria_name
        SortCriteria.Seeders -> R.string.action_sort_criteria_seeders
        SortCriteria.Peers -> R.string.action_sort_criteria_peers
        SortCriteria.FileSize -> R.string.action_sort_criteria_file_size
        SortCriteria.Date -> R.string.action_sort_criteria_date
    }

    return stringResource(id = resId)
}

@Composable
fun sortOrderStringResource(sortOrder: SortOrder): String {
    val resId = when (sortOrder) {
        SortOrder.Ascending -> R.string.action_sort_order_ascending
        SortOrder.Descending -> R.string.action_sort_order_descending
    }

    return stringResource(id = resId)
}