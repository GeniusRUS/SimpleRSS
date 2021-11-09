package com.genius.srss.di.services.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionFolderCrossRefDatabaseModel
import com.genius.srss.di.services.database.models.SubscriptionDatabaseModel
import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel

@Database(
    entities = [
        SubscriptionDatabaseModel::class,
        SubscriptionFolderDatabaseModel::class,
        SubscriptionFolderCrossRefDatabaseModel::class
    ],
    exportSchema = true,
    version = DatabaseService.DATABASE_VERSION
)
//@TypeConverters(SRSSConverters::class)
abstract class DatabaseService : RoomDatabase() {

    abstract val subscriptionsDao: SubscriptionsDao

    companion object {
        private const val DATABASE_NAME = "srss.db"
        internal const val DATABASE_VERSION = 3

        private var INSTANCE: DatabaseService? = null

        fun getDatabase(context: Context): DatabaseService {
            synchronized(DatabaseService::class.java) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext, DatabaseService::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(migrationFrom1To2, migrationFrom2to3)
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
                ?: throw IllegalStateException("Room database isn't initialized")
        }

        private val migrationFrom1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE folders ADD COLUMN isInFeedMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val migrationFrom2to3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE folders ADD COLUMN sortIndex INTEGER NOT NULL DEFAULT -1")
            }
        }
    }
}