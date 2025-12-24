package com.prajwalch.torrentsearch.data.local

import android.content.Context

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.dao.TorznabConfigDao
import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.data.local.entities.SearchHistoryEntity
import com.prajwalch.torrentsearch.data.local.entities.TorznabConfigEntity

/** Application database. */
@Database(
    entities = [
        BookmarkedTorrent::class,
        SearchHistoryEntity::class,
        TorznabConfigEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(
            from = 2,
            to = 3,
            spec = TorrentSearchDatabase.Migration2To3Spec::class,
        ),
    ],
)
abstract class TorrentSearchDatabase : RoomDatabase() {
    abstract fun bookmarkedTorrentDao(): BookmarkedTorrentDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun torznabConfigDao(): TorznabConfigDao

    @DeleteColumn.Entries(
        DeleteColumn(tableName = "bookmarks", columnName = "providerId"),
        DeleteColumn(tableName = "torznab_search_providers", columnName = "unsafeReason"),
    )
    @RenameTable(fromTableName = "torznab_search_providers", toTableName = "torznab_configs")
    class Migration2To3Spec : AutoMigrationSpec

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