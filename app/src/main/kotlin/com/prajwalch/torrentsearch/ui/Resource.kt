package com.prajwalch.torrentsearch.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.DarkTheme
import com.prajwalch.torrentsearch.domain.model.SortCriteria
import com.prajwalch.torrentsearch.domain.model.SortOrder

@Composable
fun darkThemeStringResource(darkTheme: DarkTheme): String {
    val resId = when (darkTheme) {
        DarkTheme.On -> R.string.settings_dark_theme_on
        DarkTheme.Off -> R.string.settings_dark_theme_off
        DarkTheme.FollowSystem -> R.string.settings_dark_theme_follow_system
    }

    return stringResource(id = resId)
}

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

@DrawableRes
@Composable
fun Category?.iconResId(): Int = when (this) {
    null -> R.drawable.ic_block
    Category.All -> R.drawable.ic_category
    Category.Anime -> R.drawable.ic_comic_bubble
    Category.Apps -> R.drawable.ic_widgets
    Category.Books -> R.drawable.ic_book
    Category.Games -> R.drawable.ic_sports_esports
    Category.Movies -> R.drawable.ic_movie
    Category.Music -> R.drawable.ic_music_note
    Category.Porn -> R.drawable.ic_18_up_rating
    Category.Series -> R.drawable.ic_tv
    Category.Other -> R.drawable.ic_category
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