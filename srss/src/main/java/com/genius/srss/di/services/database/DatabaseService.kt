package com.genius.srss.di.services.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        internal const val DATABASE_VERSION = 1

        private var INSTANCE: DatabaseService? = null

        fun getDatabase(context: Context): DatabaseService {
            synchronized(DatabaseService::class.java) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext, DatabaseService::class.java,
                        DATABASE_NAME
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
                ?: throw IllegalStateException("Room database isn't initialized")
        }
    }
}