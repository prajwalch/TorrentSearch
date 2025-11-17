package com.prajwalch.torrentsearch.extensions

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.parentComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable(
        route = route,
        enterTransition = { slideIntoContainer(SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        content = content,
    )
}

fun NavGraphBuilder.childComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable(
        route = route,
        enterTransition = { slideIntoContainer(SlideDirection.Start) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        content = content,
    )
}