package com.genius.srss.ui.feed

import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.INetworkSource
import com.ub.utils.LogUtils
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import javax.inject.Inject
import kotlin.properties.Delegates

class FeedPresenter @Inject constructor(
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao
): MvpPresenter<FeedView>() {

    private var state: FeedStateModel by Delegates.observable(FeedStateModel()) { _, _, newState ->
        viewState.onStateChanged(newState)
    }

    fun updateFeed(feedUrl: String) {
        presenterScope.launch {
            try {
                state = state.copy(
                    isRefreshing = true
                )
                subscriptionsDao.loadSubscriptionById(feedUrl)?.let { feed ->
                    state = state.copy(
                        title = feed.title,
                    )
                }
                val feed = networkSource.loadFeed(feedUrl) ?: return@launch
                state = state.copy(
                    feedContent = feed.items.map { item ->
                        val image = item.imageLink ?: item.enclosures.firstOrNull { it.type?.startsWith("image/") == true }?.link
                        FeedItemModel(item.id, item.link, image, item.title, item.publicationDate)
                    }
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            } finally {
                state = state.copy(
                    isRefreshing = false
                )
            }
        }
    }

    companion object {
        private const val TAG = "FeedPresenter"
    }
}