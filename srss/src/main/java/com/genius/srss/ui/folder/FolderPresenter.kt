package com.genius.srss.ui.folder

import android.content.Context
import com.genius.srss.R
import com.genius.srss.di.services.converters.SRSSConverters
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionDatabaseModel
import com.genius.srss.di.services.network.INetworkSource
import com.genius.srss.ui.subscriptions.BaseSubscriptionModel
import com.genius.srss.ui.subscriptions.FeedItemModel
import com.genius.srss.ui.subscriptions.SubscriptionFolderEmptyModel
import com.genius.srss.ui.subscriptions.SubscriptionItemModel
import com.ub.utils.LogUtils
import com.ub.utils.renew
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import kotlin.properties.Delegates

@AssistedFactory
interface FolderPresenterFactory {
    fun create(folderId: String?): FolderPresenter
}

class FolderPresenter @AssistedInject constructor(
    private val context: Context,
    private val subscriptionsDao: SubscriptionsDao,
    private val network: INetworkSource,
    private val converters: SRSSConverters,
    @Assisted folderId: String
) : MvpPresenter<FolderView>() {

    private var state: FolderStateModel by Delegates.observable(FolderStateModel(folderId = folderId)) { _, oldState, newState ->
        if (!oldState.isInEditMode && newState.isInEditMode) {
            viewState.onUpdateNameToEdit(oldState.title)
        }
        viewState.onStateChanged(newState)
    }

    private val listOfLoadingFeeds = mutableListOf<Deferred<Boolean>>()

    val isInEditMode: Boolean
        get() = state.isInEditMode

    val isInFeedListMode: Boolean
        get() = state.isCombinedMode

    fun updateFolderFeed(isManual: Boolean = false) {
        presenterScope.launch {
            try {
                updateFolderFeedInternal(isManual)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun unlinkFolderByPosition(position: Int) {
        presenterScope.launch {
            try {
                (state.feedList[position] as? SubscriptionItemModel)?.urlToLoad?.let { urlToLoad ->
                    subscriptionsDao.removeSingleCrossRefsByParameters(
                        urlToLoad = urlToLoad,
                        folderId = state.folderId
                    )
                }
                updateFolderFeed()
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

    fun checkSaveAvailability(newFolderName: String?) {
        if (!state.isInEditMode) return
        presenterScope.launch {
            try {
                state = state.copy(
                    isAvailableToSave = newFolderName?.isNotEmpty() == true && state.title != newFolderName
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun updateFolder(newFolderName: String?) {
        presenterScope.launch {
            try {
                newFolderName?.let { newName ->
                    subscriptionsDao.updateFolderNameById(state.folderId, newName)
                }
                state = state.copy(
                    isInEditMode = false
                )
                updateFolderFeedInternal(isManual = true)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun changeMode() {
        presenterScope.launch {
            try {
                if (state.isCombinedMode) {
                    listOfLoadingFeeds.forEach {
                        if (it.isActive) {
                            it.cancel()
                        }
                    }
                }
                val folderWithSubscriptions =
                    subscriptionsDao.loadFolderWithSubscriptionsById(state.folderId)
                val modifiedFolder = folderWithSubscriptions?.folder?.copy(
                    isInFeedMode = folderWithSubscriptions.folder.isInFeedMode.not()
                )
                modifiedFolder?.let { folder ->
                    subscriptionsDao.saveFolder(folder)
                    updateFolderFeedInternal(isManual = false)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun deleteFolder() {
        presenterScope.launch {
            try {
                subscriptionsDao.complexRemoveFolderById(folderId = state.folderId)
                viewState.onScreenClose()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    private suspend fun updateFolderFeedInternal(isManual: Boolean) {
        val folderWithSubscriptions =
            subscriptionsDao.loadFolderWithSubscriptionsById(state.folderId)
        if (folderWithSubscriptions?.folder?.isInFeedMode == true) {
            state = state.copy(
                isInFeedLoadingProgress = true,
                title = folderWithSubscriptions.folder.name,
                feedList = if (isManual) {
                    state.feedList
                } else {
                    emptyList()
                },
                isCombinedMode = folderWithSubscriptions.folder.isInFeedMode
            )
            val atomicUpdateFeedsList = mutableListOf<BaseSubscriptionModel>()
            parallelLoadingOfFeeds(
                subscriptions = folderWithSubscriptions.subscriptions,
                displayedFeedSource = {
                    if (isManual) {
                        atomicUpdateFeedsList
                    } else {
                        state.feedList
                    }
                },
                iterateFeedReceiver = { combinedFeedList ->
                    if (isManual) {
                        atomicUpdateFeedsList.renew(combinedFeedList)
                    } else {
                        state = state.copy(
                            feedList = combinedFeedList
                        )
                    }
                },
                exceptionHandler = { exception ->
                    LogUtils.e(TAG, exception.message, exception)
                })
            if (isManual) {
                listOfLoadingFeeds.awaitAll()
                state = state.copy(
                    feedList = atomicUpdateFeedsList
                )
            }
            val loadedFeeds = listOfLoadingFeeds.awaitAll().count { it }
            val failedToLoadListCount = listOfLoadingFeeds.size - loadedFeeds
            if (failedToLoadListCount > 0) {
                viewState.onShowLoadedFeedsCount(failedToLoadListCount)
            }
            listOfLoadingFeeds.clear()
            state = state.copy(
                isInFeedLoadingProgress = false
            )
        } else {
            state = state.copy(
                title = folderWithSubscriptions?.folder?.name,
                feedList = (folderWithSubscriptions?.subscriptions?.map {
                    SubscriptionItemModel(
                        it.title,
                        it.urlToLoad
                    )
                } ?: emptyList()).ifEmpty {
                    listOf(
                        SubscriptionFolderEmptyModel(
                            icon = R.drawable.ic_vector_empty_folder,
                            message = context.getString(R.string.subscription_folder_empty),
                            action = context.getString(R.string.subscription_folder_add_subscription)
                        )
                    )
                },
                isCombinedMode = folderWithSubscriptions?.folder?.isInFeedMode ?: false
            )
        }
    }

    private suspend fun parallelLoadingOfFeeds(
        subscriptions: List<SubscriptionDatabaseModel>?,
        displayedFeedSource: () -> (List<BaseSubscriptionModel>),
        iterateFeedReceiver: (List<BaseSubscriptionModel>) -> Unit,
        exceptionHandler: (Exception) -> Unit
    ) {
        listOfLoadingFeeds.forEach {
            if (it.isActive) {
                it.cancel()
            }
        }
        listOfLoadingFeeds.clear()
        for (subscription in subscriptions ?: emptyList()) {
            val subFeed = presenterScope.async {
                try {
                    network.loadFeed(subscription.urlToLoad)
                } catch (e: Exception) {
                    exceptionHandler.invoke(e)
                    null
                }?.let { feed ->
                    val localFeed = feed.items.map { item ->
                        converters.convertNetworkFeedToLocal(item)
                    }
                    val combinedFeed = displayedFeedSource.invoke() + localFeed
                    iterateFeedReceiver.invoke(
                        combinedFeed.sortedByDescending { item ->
                            if (item is FeedItemModel) {
                                item.publicationDate?.time
                            } else null
                        }
                    )
                    return@async true
                } ?: return@async false
            }
            listOfLoadingFeeds.add(subFeed)
        }
    }

    companion object {
        private const val TAG = "FolderPresenter"
    }
}