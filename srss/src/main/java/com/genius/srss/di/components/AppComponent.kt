package com.genius.srss.di.components

import android.content.Context
import com.genius.srss.SRSSApplication
import com.genius.srss.di.modules.AppModule
import com.genius.srss.ui.add.subscription.AddSubscriptionFragment
import com.genius.srss.ui.feed.FeedFragment
import com.genius.srss.ui.subscriptions.SubscriptionsFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    val context: Context

    fun inject(application: SRSSApplication)

    fun inject(fragment: SubscriptionsFragment)

    fun inject(fragment: FeedFragment)

    fun inject(fragment: AddSubscriptionFragment)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }
}