package com.genius.srss.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionFolderCrossRefDatabaseModel
import com.ub.utils.LogUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

class SubscriptionsViewModelFactory @Inject constructor(
    private val subscriptionDao: SubscriptionsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionsViewModel(
                subscriptionDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SubscriptionsViewModel(
    private val subscriptionDao: SubscriptionsDao
): ViewModel() {

    private val innerMainState: MutableStateFlow<SubscriptionsStateModel> = MutableStateFlow(SubscriptionsStateModel())

    private var innerState: SubscriptionsStateModel by Delegates.observable(SubscriptionsStateModel()) { _, _, newState ->
        innerMainState.value = newState
    }

    val state: StateFlow<SubscriptionsStateModel> = innerMainState

    fun updateFeed(isFull: Boolean = innerState.isFullList) {
        viewModelScope.launch {
            try {
                val folders = subscriptionDao.loadAllFolders().sortedBy { it.dateOfCreation }
                val subscriptions = if (isFull) {
                    subscriptionDao.loadSubscriptions()
                } else {
                    subscriptionDao.loadSubscriptionsWithoutFolders()
                }
                innerState = innerState.copy(
                    isFullList = isFull,
                    feedList = (folders.map {
                        SubscriptionFolderItemModel(
                            it.id,
                            it.name,
                            subscriptionDao.getCrossRefCountByFolderId(it.id)
                        )
                    } + subscriptions.map {
                        SubscriptionItemModel(
                            it.title,
                            it.urlToLoad
                        )
                    }).ifEmpty {
                        listOf(
                            SubscriptionFolderEmptyModel(
                                icon = R.drawable.ic_vector_empty_folder,
                                message = R.string.subscription_empty,
                                action = R.string.subscription_empty_first
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun removeSubscriptionByPosition(position: Int) {
        viewModelScope.launch {
            try {
                (innerState.feedList[position] as? SubscriptionItemModel)?.let { subscription ->
                    subscription.urlToLoad?.let { urlToRemove ->
                        subscriptionDao.complexRemoveSubscriptionByUrl(urlToRemove)
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            } finally {
                updateFeed()
            }
        }
    }

    fun handleHolderMove(holderPosition: Int, targetPosition: Int) {
        viewModelScope.launch {
            try {
                if (holderPosition == RecyclerView.NO_POSITION || targetPosition == RecyclerView.NO_POSITION) return@launch
                val holderToMove = innerState.feedList[holderPosition]
                val targetOfMove = innerState.feedList[targetPosition]
                when {
                    holderToMove is SubscriptionItemModel && targetOfMove is SubscriptionFolderItemModel -> {
                        subscriptionDao.saveSubscriptionFolderCrossRef(
                            SubscriptionFolderCrossRefDatabaseModel(
                                holderToMove.urlToLoad ?: return@launch,
                                targetOfMove.id
                            )
                        )
                        updateFeed()
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "SubscriptionsPresenter"
    }
}