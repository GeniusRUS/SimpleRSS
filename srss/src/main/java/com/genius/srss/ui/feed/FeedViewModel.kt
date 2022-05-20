package com.genius.srss.ui.feed

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.genius.srss.R
import com.genius.srss.di.services.converters.SRSSConverters
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.INetworkSource
import com.genius.srss.ui.subscriptions.FeedEmptyModel
import com.genius.srss.ui.subscriptions.SubscriptionFolderEmptyModel
import com.ub.utils.LogUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch

@AssistedFactory
interface FeedViewModelProvider {
    fun create(feedUrl: String): FeedViewModelFactory
}

class FeedViewModelFactory @AssistedInject constructor(
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao,
    private val converters: SRSSConverters,
    private val context: Context,
    @Assisted private val feedUrl: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(
                networkSource,
                subscriptionsDao,
                converters,
                context,
                feedUrl
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@SuppressLint("StaticFieldLeak")
class FeedViewModel(
    private val networkSource: INetworkSource,
    private val subscriptionsDao: SubscriptionsDao,
    private val converters: SRSSConverters,
    private val context: Context,
    private val feedUrl: String
) : ViewModel() {

    private val innerMainState: MutableStateFlow<FeedStateModel> =
        MutableStateFlow(FeedStateModel())
    private val innerCloseState: MutableSharedFlow<Unit> = MutableSharedFlow()
    private val innerNameToEditState: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _swipeRefreshing = MutableStateFlow(false)
    val swipeRefreshing: StateFlow<Boolean> = _swipeRefreshing

    private val _isInEditMode = MutableStateFlow(false)
    val isInEditMode: StateFlow<Boolean> = _isInEditMode.distinctUntilChanged { old, new ->
        if (!old && new) {
            innerNameToEditState.tryEmit(innerMainState.value.title)
        }
        false
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = _isInEditMode.value
    )

    val state: StateFlow<FeedStateModel> = innerMainState

    val closeFlow: SharedFlow<Unit> = innerCloseState

    val nameToEditFlow: StateFlow<String?> = innerNameToEditState

    init {
        updateFeed()
    }

    fun updateFeed() {
        viewModelScope.launch {
            try {
                _swipeRefreshing.emit(true)
                updateFeedInternal()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                innerMainState.update { state ->
                    state.copy(
                        feedContent = listOf(
                            SubscriptionFolderEmptyModel(
                                icon = R.drawable.ic_vector_warning,
                                message = context.getString(R.string.subscription_feed_error),
                                actionText = context.getString(R.string.subscription_feed_error_action)
                            )
                        )
                    )
                }
            } finally {
                _swipeRefreshing.emit(false)
            }
        }
    }

    fun deleteFeed() {
        viewModelScope.launch {
            try {
                subscriptionsDao.complexRemoveSubscriptionByUrl(feedUrl)
                innerCloseState.emit(Unit)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun changeEditMode(isEdit: Boolean) {
        viewModelScope.launch {
            _isInEditMode.emit(isEdit)
        }
    }

    fun updateSubscription(newSubscriptionName: String?) {
        viewModelScope.launch {
            try {
                _swipeRefreshing.emit(true)
                newSubscriptionName?.let { newName ->
                    subscriptionsDao.updateSubscriptionTitleByUrl(feedUrl, newName)
                }
                _isInEditMode.emit(false)
                updateFeedInternal()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
                innerMainState.update { state ->
                    state.copy(
                        feedContent = listOf(
                            FeedEmptyModel(
                                icon = R.drawable.ic_vector_warning,
                                message = context.getString(R.string.subscription_feed_error),
                                actionText = context.getString(R.string.subscription_feed_error_action)
                            )
                        )
                    )
                }
            } finally {
                _swipeRefreshing.emit(false)
            }
        }
    }

    fun checkSaveAvailability(newSubscriptionName: Editable?) {
        if (!_isInEditMode.value) return
        innerMainState.update { state ->
            state.copy(
                isAvailableToSave = newSubscriptionName?.isNotEmpty() == true && innerMainState.value.title != newSubscriptionName.toString()
            )
        }
    }

    private suspend fun updateFeedInternal() {
        subscriptionsDao.loadSubscriptionById(feedUrl)?.let { feed ->
            innerMainState.update { state ->
                state.copy(
                    title = feed.title
                )
            }
        }
        val feed =
            networkSource.loadFeed(feedUrl) ?: throw NullPointerException("Parsed feed is null")
        innerMainState.update { state ->
            state.copy(
                feedContent = feed.items.map { item ->
                    converters.convertNetworkFeedToLocal(item)
                }.sortedByDescending { feedItem ->
                    feedItem.timestamp?.date?.time
                }.ifEmpty {
                    listOf(
                        FeedEmptyModel(
                            icon = R.drawable.ic_vector_empty_folder,
                            message = context.getString(R.string.subscription_feed_empty)
                        )
                    )
                }
            )
        }
    }

    companion object {
        private const val TAG = "FeedPresenter"
    }
}