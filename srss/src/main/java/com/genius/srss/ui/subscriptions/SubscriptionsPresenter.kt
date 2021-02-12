package com.genius.srss.ui.subscriptions

import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.ub.utils.LogUtils
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import javax.inject.Inject
import kotlin.properties.Delegates

class SubscriptionsPresenter @Inject constructor(
    private val subscriptionDao: SubscriptionsDao
): MvpPresenter<SubscriptionsView>() {

    private var state: SubscriptionsStateModel by Delegates.observable(SubscriptionsStateModel()) { _, _, newState ->
        viewState.onStateChanged(newState)
    }

    fun updateFeed() {
        presenterScope.launch {
            try {
                val subscriptions = subscriptionDao.loadSubscriptions()
                state = state.copy(
                    feedList = subscriptions.map {
                        SubscriptionItemModel(
                            it.title,
                            it.urlToLoad
                        )
                    }
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun removeSubscriptionByPosition(position: Int) {
        presenterScope.launch {
            try {
                state.feedList[position].urlToLoad?.let { urlToRemove ->
                    subscriptionDao.complexRemoveSubscriptionByUrl(urlToRemove)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            } finally {
                updateFeed()
            }
        }
    }

    companion object {
        private const val TAG = "SubscriptionsPresenter"
    }
}