package com.prajwalch.torrentsearch.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.prajwalch.torrentsearch.database.entities.BookmarkedTorrent

import com.prajwalch.torrentsearch.database.entities.SearchHistory

/**
 * Database public implementation.
 *
 * Every other components who wants to use database should receive this instead
 * of [DatabaseDao].
 */
class TorrentSearchDatabase(private val internalDatabase: InternalDatabase) :
    DatabaseDao by internalDatabase.databaseDao() {

    /**
     * Closes the database.
     *
     * Once closed it shouldn't be used.
     */
    fun close() {
        internalDatabase.close()
    }
}

/**
 * Database internal implementation.
 *
 * It is responsible for creating database and maintaining the single instance
 * therefore it shouldn't be used directly.
 */
@Database(
    entities = [
        SearchHistory::class,
        BookmarkedTorrent::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class InternalDatabase : RoomDatabase() {
    abstract fun databaseDao(): DatabaseDao

    companion object {
        /** Name of the database file. */
        private const val DB_NAME = "torrentsearch.db"

        /**
         * Single instance of the database.
         *
         * Recommended to re-use the reference once database is created.
         */
        private var Instance: InternalDatabase? = null

        /** Returns the instance of the database. */
        fun getInstance(context: Context): InternalDatabase {
            return Instance ?: createInstance(context = context)
        }

        /** Creates, stores and returns the instance of the database. */
        private fun createInstance(context: Context): InternalDatabase {
            val databaseBuilder = Room.databaseBuilder(
                context = context,
                klass = InternalDatabase::class.java,
                name = DB_NAME,
            )

            return databaseBuilder.build().also { Instance = it }
        }
    }
}