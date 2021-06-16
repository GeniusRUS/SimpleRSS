package com.genius.srss.ui.feed

import com.genius.srss.R
import com.genius.srss.di.DIManager
import com.genius.srss.di.services.converters.SRSSConverters
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.INetworkSource
import com.genius.srss.ui.subscriptions.SubscriptionFolderEmptyModel
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
                state = state.copy(
                    feedContent = listOf(
                        SubscriptionFolderEmptyModel(
                            icon = R.drawable.ic_vector_warning,
                            message = DIManager.appComponent.context.getString(R.string.subscription_feed_error),
                            action = DIManager.appComponent.context.getString(R.string.subscription_feed_error_action)
                        )
                    )
                )
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
                state = state.copy(
                    isInEditMode = false
                )
                updateFeedInternal()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                state = state.copy(
                    feedContent = listOf(
                        SubscriptionFolderEmptyModel(
                            icon = R.drawable.ic_vector_warning,
                            message = DIManager.appComponent.context.getString(R.string.subscription_feed_error),
                            action = DIManager.appComponent.context.getString(R.string.subscription_feed_error_action)
                        )
                    )
                )
            } finally {
                state = state.copy(
                    isRefreshing = false
                )
            }
        }
    }

    fun checkSaveAvailability(newSubscriptionName: String?) {
        if (!state.isInEditMode) return
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
        val feed = networkSource.loadFeed(feedUrl) ?: throw NullPointerException("Parsed feed is null")
        state = state.copy(
            feedContent = feed.items.map { item ->
                converters.convertNetworkFeedToLocal(item)
            }.sortedByDescending { feedItem ->
                feedItem.publicationDate?.time
            }.ifEmpty {
                listOf(
                    SubscriptionFolderEmptyModel(
                        icon = R.drawable.ic_vector_empty_folder,
                        message = DIManager.appComponent.context.getString(R.string.subscription_feed_empty)
                    )
                )
            }
        )
    }

    companion object {
        private const val TAG = "FeedPresenter"
    }
}