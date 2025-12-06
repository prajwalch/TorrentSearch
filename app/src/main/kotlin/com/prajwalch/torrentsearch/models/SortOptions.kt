package com.prajwalch.torrentsearch.models

data class SortOptions(
    val criteria: SortCriteria = SortCriteria.Default,
    val order: SortOrder = SortOrder.Default,
)