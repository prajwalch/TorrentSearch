package com.prajwalch.torrentsearch.data.local

import android.content.Context

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.dao.TorznabConfigDao
import com.prajwalch.torrentsearch.data.local.dao.ViewedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrent
import com.prajwalch.torrentsearch.data.local.entities.SearchHistoryEntity
import com.prajwalch.torrentsearch.data.local.entities.TorznabConfigEntity
import com.prajwalch.torrentsearch.data.local.entities.ViewedTorrentEntity

/** Application database. */
@Database(
    entities = [
        BookmarkedTorrent::class,
        SearchHistoryEntity::class,
        TorznabConfigEntity::class,
        ViewedTorrentEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(
            from = 2,
            to = 3,
            spec = TorrentSearchDatabase.Migration2To3Spec::class,
        ),
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class TorrentSearchDatabase : RoomDatabase() {
    abstract fun bookmarkedTorrentDao(): BookmarkedTorrentDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun torznabConfigDao(): TorznabConfigDao

    abstract fun viewedTorrentDao(): ViewedTorrentDao

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
            val databaseBuilder = Room
                .databaseBuilder(
                    context = context,
                    klass = TorrentSearchDatabase::class.java,
                    name = DB_NAME,
                )
                .addMigrations(MIGRATION_4_5)

            return databaseBuilder.build().also { Instance = it }
        }
    }
}

/**
 * Migration from version 4 to 5:
 * - Changes `bookmarks.id` from `Long` to `String` (UUID based on infoHash).
 * - Creates a new viewed_torrents table.
 */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create a new bookmarks table with an id of type string.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bookmarks_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                size TEXT NOT NULL,
                seeders INTEGER NOT NULL,
                peers INTEGER NOT NULL,
                providerName TEXT NOT NULL,
                uploadDate TEXT NOT NULL,
                category TEXT NOT NULL,
                descriptionPageUrl TEXT NOT NULL,
                magnetUri TEXT NOT NULL,
                fileDownloadLink TEXT DEFAULT NULL
            )
            """.trimIndent()
        )

        // 2. Copy old bookmarks data, generating new id for each bookmark (UUID-like hex string).
        db.execSQL(
            """
            INSERT INTO bookmarks_new (id, name, size, seeders, peers, providerName, 
                uploadDate, category, descriptionPageUrl, magnetUri, fileDownloadLink)
            SELECT 
                lower(hex(randomblob(16))),
                name, size, seeders, peers, providerName, 
                uploadDate, category, descriptionPageUrl, magnetUri, fileDownloadLink
            FROM bookmarks
            """.trimIndent()
        )

        // 3. Drop old bookmarks table.
        db.execSQL("DROP TABLE bookmarks")

        // 4. Rename new bookmarks table same as the old one.
        db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")

        // 5. Create unique index on name (must be after rename)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarks_name ON bookmarks (name)")

        // 6. Create a new viewed_torrents table.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS viewed_torrents (
                id TEXT NOT NULL PRIMARY KEY,
                viewedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}