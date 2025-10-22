package com.prajwalch.torrentsearch.data.database

import android.content.Context
import androidx.room.AutoMigration

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.prajwalch.torrentsearch.data.database.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.database.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.database.dao.TorznabSearchProviderDao
import com.prajwalch.torrentsearch.data.database.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.data.database.entities.SearchHistoryEntity
import com.prajwalch.torrentsearch.data.database.entities.TorznabSearchProviderEntity

/** Application database. */
@Database(
    entities = [
        BookmarkedTorrent::class,
        SearchHistoryEntity::class,
        TorznabSearchProviderEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class TorrentSearchDatabase : RoomDatabase() {
    abstract fun bookmarkedTorrentDao(): BookmarkedTorrentDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun torznabSearchProviderDao(): TorznabSearchProviderDao

    companion object {
        /** Name of the database file. */
        private const val DB_NAME = "torrentsearch.db"

        /**
         * Single instance of the database.
         *
         * Recommended to re-use the reference once database is created.
         */
        private var Instance: TorrentSearchDatabase? = null

        /** Returns the instance of the database. */
        fun getInstance(context: Context): TorrentSearchDatabase {
            return Instance ?: createInstance(context = context)
        }

        /** Creates, stores and returns the instance of the database. */
        private fun createInstance(context: Context): TorrentSearchDatabase {
            val databaseBuilder = Room.databaseBuilder(
                context = context,
                klass = TorrentSearchDatabase::class.java,
                name = DB_NAME,
            )

            return databaseBuilder.build().also { Instance = it }
        }
    }
}