package com.prajwalch.torrentsearch.ui.settings.searchproviders.cloudflare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.providers.SearchProviderId

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudflareViewModel @Inject constructor(
    private val searchProvidersManager: SearchProvidersManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val url = savedStateHandle.get<String>("url")!!

    fun markProviderAsUnlocked() {
        viewModelScope.launch {
            savedStateHandle.get<SearchProviderId>("id")?.let {
                searchProvidersManager.unlockProvider(it)
            }
        }
    }
}