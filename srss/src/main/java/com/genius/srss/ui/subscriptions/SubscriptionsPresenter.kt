package com.genius.srss.ui.subscriptions

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionFolderCrossRefDatabaseModel
import com.ub.utils.LogUtils
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import javax.inject.Inject
import kotlin.properties.Delegates

class SubscriptionsPresenter @Inject constructor(
    private val context: Context,
    private val subscriptionDao: SubscriptionsDao
): MvpPresenter<SubscriptionsView>() {

    private var state: SubscriptionsStateModel by Delegates.observable(SubscriptionsStateModel()) { _, _, newState ->
        viewState.onStateChanged(newState)
    }

    fun updateFeed(isFull: Boolean = state.isFullList) {
        presenterScope.launch {
            try {
                val folders = subscriptionDao.loadAllFolders().sortedBy { it.dateOfCreation }
                val subscriptions = if (isFull) {
                    subscriptionDao.loadSubscriptions()
                } else {
                    subscriptionDao.loadSubscriptionsWithoutFolders()
                }
                state = state.copy(
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
                                message = context.getString(R.string.subscription_empty),
                                action = context.getString(R.string.subscription_empty_first)
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
        presenterScope.launch {
            try {
                (state.feedList[position] as? SubscriptionItemModel)?.let { subscription ->
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
        presenterScope.launch {
            try {
                if (holderPosition == RecyclerView.NO_POSITION || targetPosition == RecyclerView.NO_POSITION) return@launch
                val holderToMove = state.feedList[holderPosition]
                val targetOfMove = state.feedList[targetPosition]
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