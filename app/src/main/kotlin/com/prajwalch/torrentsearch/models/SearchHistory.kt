package com.prajwalch.torrentsearch.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class SearchHistoryId(val value: Long) : Parcelable

data class SearchHistory(
    val id: SearchHistoryId = SearchHistoryId(0),
    val query: String,
)