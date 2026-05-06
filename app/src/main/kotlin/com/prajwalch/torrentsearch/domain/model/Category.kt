package com.prajwalch.torrentsearch.domain.model

enum class Category(val isNSFW: Boolean = false) {
    All,
    Anime,
    Apps,
    Books,
    Games,
    Movies,
    Music,
    Porn(isNSFW = true),
    Series,
    Other,
}