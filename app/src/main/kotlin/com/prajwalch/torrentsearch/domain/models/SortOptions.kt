package com.prajwalch.torrentsearch.domain.models

data class SortOptions(
    val criteria: SortCriteria = SortCriteria.Default,
    val order: SortOrder = SortOrder.Default,
)