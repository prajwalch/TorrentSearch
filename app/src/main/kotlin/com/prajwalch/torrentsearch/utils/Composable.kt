package com.prajwalch.torrentsearch.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.models.Category

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

    return stringResource(resId)
}