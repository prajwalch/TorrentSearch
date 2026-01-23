package com.prajwalch.torrentsearch.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prajwalch.torrentsearch.R
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.SortCriteria
import com.prajwalch.torrentsearch.domain.models.SortOrder
import com.prajwalch.torrentsearch.domain.models.TorznabConnectionCheckResult

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

@Composable
fun torznabConnectionCheckResultStringResource(result: TorznabConnectionCheckResult): String {
    if (result is TorznabConnectionCheckResult.InternalApplicationError) {
        return stringResource(
            R.string.torznab_conn_check_result_internal_app_error,
            result.errorCode,
        )
    }

    val otherResId = when (result) {
        TorznabConnectionCheckResult.ApiDisabled -> R.string.torznab_conn_check_result_api_disabled
        TorznabConnectionCheckResult.CannotConnect -> R.string.torznab_conn_check_result_cannot_connect
        TorznabConnectionCheckResult.InvalidApiKey -> R.string.torznab_conn_check_result_invalid_api_key
        TorznabConnectionCheckResult.Ok -> R.string.torznab_conn_check_result_ok
        TorznabConnectionCheckResult.UnknownError -> R.string.torznab_conn_check_result_unknown_error
    }

    return stringResource(otherResId)
}