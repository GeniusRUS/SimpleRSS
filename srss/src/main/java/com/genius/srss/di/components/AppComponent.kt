package com.genius.srss.di.components

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.genius.srss.SRSSApplication
import com.genius.srss.di.modules.AppModule
import com.genius.srss.di.services.converters.SRSSConverters
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.NetworkService
import com.genius.srss.ui.add.folder.AddFolderFragment
import com.genius.srss.ui.add.subscription.AddSubscriptionFragment
import com.genius.srss.ui.feed.FeedFragment
import com.genius.srss.ui.folder.FolderFragment
import com.genius.srss.ui.subscriptions.SubscriptionsFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    val context: Context

    val network: NetworkService

    val subscriptionDao: SubscriptionsDao

    val dataStore: DataStore<Preferences>

    val converters: SRSSConverters

    fun inject(application: SRSSApplication)

    fun inject(fragment: SubscriptionsFragment)

    fun inject(fragment: FeedFragment)

    fun inject(fragment: AddSubscriptionFragment)

    fun inject(fragment: AddFolderFragment)

    fun inject(fragment: FolderFragment)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }
}