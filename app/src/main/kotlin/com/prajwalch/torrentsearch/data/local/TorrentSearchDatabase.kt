package com.prajwalch.torrentsearch.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.prajwalch.torrentsearch.data.local.dao.BookmarkedTorrentDao
import com.prajwalch.torrentsearch.data.local.dao.SearchHistoryDao
import com.prajwalch.torrentsearch.data.local.dao.TorznabConfigDao
import com.prajwalch.torrentsearch.data.local.dao.ViewedTorrentDao
import com.prajwalch.torrentsearch.data.local.entities.BookmarkedTorrentEntity
import com.prajwalch.torrentsearch.data.local.entities.SearchHistoryEntity
import com.prajwalch.torrentsearch.data.local.entities.TorznabConfigEntity
import com.prajwalch.torrentsearch.data.local.entities.ViewedTorrentEntity
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.providers.XXXTracker
import com.prajwalch.torrentsearch.util.TorrentDateParser
import com.prajwalch.torrentsearch.util.TorrentUtils

import java.time.Instant

/** Application database. */
@Database(
    entities = [
        BookmarkedTorrentEntity::class,
        SearchHistoryEntity::class,
        TorznabConfigEntity::class,
        ViewedTorrentEntity::class,
    ],
    version = 6,
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
@TypeConverters(Converters::class)
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
                .addMigrations(MIGRATION_5_6)

            return databaseBuilder.build().also { Instance = it }
        }
    }
}

/**
 * Migration from version 4 to 5:
 * - Adds new `bookmarks.infoHash` field.
 * - Changes `bookmarks.uploadDate` from `String` to `Long` representing [java.time.Instant].
 * - Creates a new `viewed_torrents` table.
 */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Rename old bookmarks table.
        db.execSQL("ALTER TABLE bookmarks RENAME TO bookmarks_old")

        // 2. Create new bookmarks table with updated fields.
        // language="RoomSql"
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `infoHash` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `size` TEXT NOT NULL,
                `seeders` INTEGER NOT NULL,
                `peers` INTEGER NOT NULL,
                `providerName` TEXT NOT NULL,
                `uploadDate` INTEGER DEFAULT NULL,
                `category` TEXT NOT NULL,
                `descriptionPageUrl` TEXT NOT NULL,
                `magnetUri` TEXT DEFAULT NULL,
                `fileDownloadLink` TEXT DEFAULT NULL
            )
            """.trimIndent()
        )

        // 3. Migrate bookmarks from old table to new one.
        db.query("SELECT * FROM bookmarks_old").use { cursor ->
            while (cursor.moveToNext()) {
                val magnetUri = cursor.getString(cursor.getColumnIndexOrThrow("magnetUri"))
                // Previously, due to an issue some bookmarks magnet URI got ended up
                // with 'null' string.
                //
                // See https://github.com/prajwalch/TorrentSearch/pull/94.
                if (magnetUri == "null") continue

                val infoHash = runCatching { TorrentUtils.getInfoHashFromMagnetUri(magnetUri) }
                    .getOrNull() ?: continue
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val size = cursor.getString(cursor.getColumnIndexOrThrow("size"))
                val seeders = cursor.getInt(cursor.getColumnIndexOrThrow("seeders"))
                val peers = cursor.getInt(cursor.getColumnIndexOrThrow("peers"))
                val providerName = cursor.getString(cursor.getColumnIndexOrThrow("providerName"))
                val uploadDate = cursor.getString(cursor.getColumnIndexOrThrow("uploadDate"))
                val newUploadDate = runCatching {
                    parseOldTorrentUploadDate(
                        date = uploadDate,
                        providerName = providerName,
                    )?.toEpochMilli()
                }.getOrNull()
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val descriptionPageUrl = cursor
                    .getString(cursor.getColumnIndexOrThrow("descriptionPageUrl"))

                val fileDownloadLinkIndex = cursor.getColumnIndexOrThrow("fileDownloadLink")
                val fileDownloadLink = if (cursor.isNull(fileDownloadLinkIndex)) {
                    null
                } else {
                    cursor.getString(fileDownloadLinkIndex)
                }

                val columnValues = ContentValues().apply {
                    put("infoHash", infoHash)
                    put("name", name)
                    put("size", size)
                    put("seeders", seeders)
                    put("peers", peers)
                    put("providerName", providerName)
                    put("uploadDate", newUploadDate)
                    put("category", category)
                    put("descriptionPageUrl", descriptionPageUrl)
                    put("magnetUri", magnetUri)
                    put("fileDownloadLink", fileDownloadLink)
                }
                db.insert("bookmarks", SQLiteDatabase.CONFLICT_REPLACE, columnValues)
            }
        }

        // 4. Drop old bookmarks table.
        db.execSQL("DROP TABLE bookmarks_old")

        // 5. Create a new viewed_torrents table.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS viewed_torrents (
                id TEXT PRIMARY KEY NOT NULL,
                viewedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun parseOldTorrentUploadDate(
        date: String,
        providerName: String,
    ): Instant? = when (providerName) {
        // Eztv uses weird formatting on top of that it's now Cloudflare protected.
        // AniRena and FileMood date parsing was not involved.
        "AniRena", "Eztv", "FileMood" -> null

        "AnimeTosho",
        "BitSearch",
        "Dmhy",
        "InternetArchive",
        "Knaben",
        "Nyaa",
        "SubsPlease",
        "Sukebei",
        "ThePirateBay",
        "TheRarBg",
        "TokyoToshokan",
        "TorrentDatabase",
        "TorrentDownloads",
        "TorrentsCSV",
        "XXXClub",
        "Yts",
            -> TorrentDateParser.parse(date = date, format = "dd MMM yyyy")

        "LimeTorrents",
        "MyPornClub",
        "TorrentDownload",
        "UIndex",
            -> TorrentDateParser.tryParseRelative(date)

        "XXXTracker" -> TorrentDateParser.parse(
            date = XXXTracker.normalizeUploadDate(date),
            format = "dd MMM yy",
        )

        else -> null
    }
}

/**
 * Migration from version 5 to 6:
 * - Changes `bookmarks.size` column from not nullable to nullable.
 * - Changes `bookmarks.seeders` column from not nullable to nullable.
 * - Changes `bookmarks.peers` column from not nullable to nullable.
 * - Changes `bookmarks.category` column from not nullable to nullable.
 * - Changes `bookmarks.descriptionPageUrl` column from not nullable to nullable.
 * - Changes `torznab_configs.category` to `supported_categories`.
 */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateBookmarks(db)
        migrateTorznabConfigs(db)
    }

    private fun migrateBookmarks(db: SupportSQLiteDatabase) {
        // 1. Rename old bookmarks table
        db.execSQL("ALTER TABLE bookmarks RENAME TO bookmarks_old")

        // 2. Create new bookmarks table with the `size` field nullable
        // language="RoomSql"
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `infoHash` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `size` TEXT DEFAULT NULL,
                `seeders` INTEGER DEFAULT NULL,
                `peers` INTEGER DEFAULT NULL,
                `providerName` TEXT NOT NULL,
                `uploadDate` INTEGER DEFAULT NULL,
                `category` TEXT DEFAULT NULL,
                `descriptionPageUrl` TEXT DEFAULT NULL,
                `magnetUri` TEXT DEFAULT NULL,
                `fileDownloadLink` TEXT DEFAULT NULL
            )
            """.trimIndent()
        )

        // 3. Migrate bookmarks from old table to new one.
        // language="RoomSql"
        db.execSQL(
            """
            INSERT INTO bookmarks (id, infoHash, name, size, seeders, peers, providerName, uploadDate, category, descriptionPageUrl, magnetUri, fileDownloadLink)
            SELECT id, infoHash, name, size, seeders, peers, providerName, uploadDate, NULLIF(category,''), NULLIF(descriptionPageUrl, ''), magnetUri, fileDownloadLink FROM bookmarks_old
        """.trimIndent()
        )

        // 4. Drop old bookmarks table.
        db.execSQL("DROP TABLE bookmarks_old")
    }

    private fun migrateTorznabConfigs(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE torznab_configs RENAME TO torznab_configs_old")
        // language="RoomSql"
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `torznab_configs` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                `apiKey` TEXT NOT NULL,
                `supported_categories` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.query("SELECT * FROM torznab_configs_old").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                val apikey = cursor.getString(cursor.getColumnIndexOrThrow("apiKey"))
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                    ?.let { runCatching { Category.valueOf(it) }.getOrNull() }
                val supportedCategories = when (category) {
                    null -> emptySet()
                    Category.All -> Category.entries.filter { it != Category.All }.toSet()
                    else -> setOf(category)
                }

                val columnValues = ContentValues().apply {
                    put("id", id)
                    put("name", name)
                    put("url", url)
                    put("apiKey", apikey)
                    put("supported_categories", Converters.categorySetToString(supportedCategories))
                }

                db.insert("torznab_configs", SQLiteDatabase.CONFLICT_IGNORE, columnValues)
            }
        }
        db.execSQL("DROP TABLE torznab_configs_old")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_torznab_configs_id` ON `torznab_configs` (`id`)")
    }
}