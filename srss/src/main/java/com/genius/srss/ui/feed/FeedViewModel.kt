package com.genius.srss.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.genius.srss.R
import com.genius.srss.di.services.converters.SRSSConverters
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.INetworkSource
import com.genius.srss.ui.subscriptions.SubscriptionFolderEmptyModel
import com.ub.utils.LogUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AssistedFactory
interface FeedViewModelProvider {
    fun create(feedUrl: String): FeedViewModelFactory
}

class FeedViewModelFactory @AssistedInject constructor(
    private val context: Context,
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao,
    private val converters: SRSSConverters,
    @Assisted private val feedUrl: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(
                context,
                networkSource,
                subscriptionsDao,
                converters,
                feedUrl
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class FeedViewModel(
    private val context: Context,
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao,
    private val converters: SRSSConverters,
    private val feedUrl: String
): ViewModel() {

    private val innerMainState: MutableStateFlow<FeedStateModel> = MutableStateFlow(FeedStateModel())
    private val innerCloseState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val innerNameToEditState: MutableStateFlow<String?> = MutableStateFlow(null)

    private var innerState: FeedStateModel by Delegates.observable(innerMainState.value) { _, oldState, newState ->
        if (!oldState.isInEditMode && newState.isInEditMode) {
            innerNameToEditState.value = oldState.title
        }
        innerMainState.value = newState
    }

    val state: StateFlow<FeedStateModel> = innerMainState

    val closeFlow: StateFlow<Boolean> = innerCloseState

    val nameToEditFlow: StateFlow<String?> = innerNameToEditState

    val isInEditMode: Boolean
        get() = innerState.isInEditMode

    fun updateFeed() {
        viewModelScope.launch {
            try {
                innerState = innerState.copy(
                    isRefreshing = true
                )
                updateFeedInternal()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                innerState = innerState.copy(
                    feedContent = listOf(
                        SubscriptionFolderEmptyModel(
                            icon = R.drawable.ic_vector_warning,
                            message = context.getString(R.string.subscription_feed_error),
                            action = context.getString(R.string.subscription_feed_error_action)
                        )
                    )
                )
            } finally {
                innerState = innerState.copy(
                    isRefreshing = false
                )
            }
        }
    }

    fun deleteFeed() {
        viewModelScope.launch {
            try {
                subscriptionsDao.complexRemoveSubscriptionByUrl(feedUrl)
                innerCloseState.value = true
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun changeEditMode(isEdit: Boolean) {
        innerState = innerState.copy(
            isInEditMode = isEdit
        )
    }

    fun updateSubscription(newSubscriptionName: String?) {
        viewModelScope.launch {
            try {
                innerState = innerState.copy(
                    isRefreshing = true
                )
                newSubscriptionName?.let { newName ->
                    subscriptionsDao.updateSubscriptionTitleByUrl(feedUrl, newName)
                }
                innerState = innerState.copy(
                    isInEditMode = false
                )
                updateFeedInternal()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                innerState = innerState.copy(
                    feedContent = listOf(
                        SubscriptionFolderEmptyModel(
                            icon = R.drawable.ic_vector_warning,
                            message = context.getString(R.string.subscription_feed_error),
                            action = context.getString(R.string.subscription_feed_error_action)
                        )
                    )
                )
            } finally {
                innerState = innerState.copy(
                    isRefreshing = false
                )
            }
        }
    }

    fun checkSaveAvailability(newSubscriptionName: String?) {
        if (!innerState.isInEditMode) return
        viewModelScope.launch {
            try {
                innerState = innerState.copy(
                    isAvailableToSave = newSubscriptionName?.isNotEmpty() == true && innerState.title != newSubscriptionName
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    private suspend fun updateFeedInternal() {
        subscriptionsDao.loadSubscriptionById(feedUrl)?.let { feed ->
            innerState = innerState.copy(
                title = feed.title,
            )
        }
        val feed = networkSource.loadFeed(feedUrl) ?: throw NullPointerException("Parsed feed is null")
        innerState = innerState.copy(
            feedContent = feed.items.map { item ->
                converters.convertNetworkFeedToLocal(item)
            }.sortedByDescending { feedItem ->
                feedItem.publicationDate?.time
            }.ifEmpty {
                listOf(
                    SubscriptionFolderEmptyModel(
                        icon = R.drawable.ic_vector_empty_folder,
                        message = context.getString(R.string.subscription_feed_empty)
                    )
                )
            }
        )
    }

    companion object {
        private const val TAG = "FeedPresenter"
    }
}