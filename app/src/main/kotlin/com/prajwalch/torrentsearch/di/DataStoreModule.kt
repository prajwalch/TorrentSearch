package com.prajwalch.torrentsearch.di

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

val dataStoreModule = module {
    single<DataStore<Preferences>> { androidContext().settingsDataStore }
}
