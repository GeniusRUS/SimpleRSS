package com.genius.srss.di.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.genius.srss.BuildConfig
import com.genius.srss.di.services.database.DatabaseService
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.NetworkService
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
object CoreModule {

    @Singleton
    @Provides
    fun provideNetworkCoroutineDispatcher(): CoroutineContext = Dispatchers.IO

    @Provides
    @Singleton
    @Named(value = "logging_interceptor")
    fun provideLoggingInterceptor(): Interceptor = HttpLoggingInterceptor().setLevel(
        when {
            BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BASIC
            else -> HttpLoggingInterceptor.Level.NONE
        }
    )

    @Provides
    @Singleton
    fun provideDatabaseService(context: Context): DatabaseService =
        DatabaseService.getDatabase(context)

    @Provides
    @Singleton
    fun provideSubscriptionsDao(databaseService: DatabaseService): SubscriptionsDao = databaseService.subscriptionsDao

    @Provides
    @Singleton
    fun provideOkHttpClient(apiService: NetworkService): OkHttpClient = apiService.okHttpClient

    @Provides
    @Singleton
    fun provideDataStore(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile("srss")
            }
        )
}