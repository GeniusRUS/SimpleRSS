package com.genius.srss.ui.feed

import com.einmalfel.earl.RSSFeed
import com.einmalfel.earl.RSSItem
import com.genius.srss.R
import com.genius.srss.di.services.converters.SRSSConverters
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.INetworkSource
import com.ub.utils.LogUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import kotlin.properties.Delegates

@AssistedFactory
interface FeedPresenterProvider {
    fun create(feedUrl: String): FeedPresenter
}

class FeedPresenter @AssistedInject constructor(
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao,
    private val converters: SRSSConverters,
    @Assisted private val feedUrl: String
): MvpPresenter<FeedView>() {

    private var state: FeedStateModel by Delegates.observable(FeedStateModel()) { _, oldState, newState ->
        if (!oldState.isInEditMode && newState.isInEditMode) {
            viewState.onUpdateNameToEdit(oldState.title)
        }
        viewState.onStateChanged(newState)
    }

    val isInEditMode: Boolean
        get() = state.isInEditMode

    fun updateFeed() {
        presenterScope.launch {
            try {
                state = state.copy(
                    isRefreshing = true
                )
                updateFeedInternal()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                viewState.onShowError(R.string.subscription_feed_error)
            } finally {
                state = state.copy(
                    isRefreshing = false
                )
            }
        }
    }

    fun deleteFeed() {
        presenterScope.launch {
            try {
                subscriptionsDao.complexRemoveSubscriptionByUrl(feedUrl)
                viewState.onScreenClose()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun changeEditMode(isEdit: Boolean) {
        state = state.copy(
            isInEditMode = isEdit
        )
    }

    fun updateSubscription(newSubscriptionName: String?) {
        presenterScope.launch {
            try {
                state = state.copy(
                    isRefreshing = true
                )
                newSubscriptionName?.let { newName ->
                    subscriptionsDao.updateSubscriptionTitleByUrl(feedUrl, newName)
                }
                updateFeedInternal()
                state = state.copy(
                    isInEditMode = false
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                viewState.onShowError(R.string.subscription_feed_error)
            } finally {
                state = state.copy(
                    isRefreshing = false
                )
            }
        }
    }

    fun checkSaveAvailability(newSubscriptionName: String?) {
        presenterScope.launch {
            try {
                state = state.copy(
                    isAvailableToSave = newSubscriptionName?.isNotEmpty() == true && state.title != newSubscriptionName
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    private suspend fun updateFeedInternal() {
        subscriptionsDao.loadSubscriptionById(feedUrl)?.let { feed ->
            state = state.copy(
                title = feed.title,
            )
        }
        val feed = networkSource.loadFeed(feedUrl) ?: return
        state = state.copy(
            feedContent = feed.items.map { item ->
                val image = item.imageLink
                    ?: item.enclosures.firstOrNull { it.type?.startsWith("image/") == true }?.link
                    ?: converters.extractImageUrlFromText(item.description)
                FeedItemModel(item.id, item.link, image, item.title, item.publicationDate)
            }
        )
    }

    companion object {
        private const val TAG = "FeedPresenter"
    }
}