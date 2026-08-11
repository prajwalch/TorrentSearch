package com.prajwalch.torrentsearch.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** Discovers `@KoinViewModel`-annotated classes under the `ui` package. */
@Module
@ComponentScan("com.prajwalch.torrentsearch.ui")
class ViewModelModule
