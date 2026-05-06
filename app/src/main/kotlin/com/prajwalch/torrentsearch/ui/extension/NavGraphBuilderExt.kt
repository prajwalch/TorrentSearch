package com.prajwalch.torrentsearch.ui.extension

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

inline fun <reified T : Any> NavGraphBuilder.parentComposable(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable<T>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Companion.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Companion.Start) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Companion.End) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Companion.End) },
        content = content,
    )
}

inline fun <reified T : Any> NavGraphBuilder.childComposable(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable<T>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Companion.Start) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Companion.End) },
        content = content,
    )
}