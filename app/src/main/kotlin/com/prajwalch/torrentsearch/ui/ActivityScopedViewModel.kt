package com.prajwalch.torrentsearch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner

@Composable
inline fun <reified VM : ViewModel> activityScopedViewModel(): VM {
    val context = LocalContext.current as? ViewModelStoreOwner
    return hiltViewModel(viewModelStoreOwner = checkNotNull(context))
}