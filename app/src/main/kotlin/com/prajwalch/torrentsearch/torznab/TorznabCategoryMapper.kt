package com.prajwalch.torrentsearch.torznab

import com.prajwalch.torrentsearch.domain.model.Category

object TorznabCategoryMapper {
    /**
     * [Category] to Torznab category ids map.
     *
     * The mapped ids are used only inside the request URL.
     *
     * See:
     * - https://github.com/Jackett/Jackett/wiki/Jackett-Categories
     * - https://newznab.readthedocs.io/en/latest/misc/api.html#predefined-categories
     */
    private val categoryToIdsMap = mapOf(
        Category.Anime to listOf(5070),
        Category.Apps to listOf(4000, 4010, 4020, 4030, 4040, 4050, 4060, 4070),
        Category.Books to listOf(7000, 7010, 7020, 7030, 7040, 7050, 7060),
        Category.Games to listOf(1000, 4050),
        Category.Movies to listOf(2000, 2010, 2020, 2030, 2040, 2045, 2050, 2060, 2070, 2080),
        Category.Music to listOf(3000, 3010, 3040, 3050, 3060),
        Category.Porn to listOf(6000, 6010, 6020, 6030, 6040, 6045, 6050, 6060, 6070, 6080, 6090),
        Category.Series to listOf(5000, 5010, 5020, 5030, 5040, 5045, 5050, 5060, 5080),
        Category.Other to listOf(8000, 8010, 8020),
    )

    fun getCatParamValue(category: Category): String? {
        if (category == Category.All) return null
        return categoryToIdsMap[category]?.joinToString()
    }

    fun getCategoryFromId(id: Int): Category = when (id) {
        in 1000..1999 -> Category.Games
        in 2000..2999 -> Category.Movies
        in 3000..3999 -> if (id == 3030) Category.Books else Category.Music
        in 4000..4999 -> if (id == 4050) Category.Games else Category.Apps
        in 5000..5999 -> if (id == 5070) Category.Anime else Category.Series
        in 6000..6999 -> Category.Porn
        in 7000..7999 -> Category.Books
        in 8000..8999 -> Category.Other
        else -> Category.Other
    }
}