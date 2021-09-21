package com.genius.srss.ui.folder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AssistedFactory
interface FolderViewModelFactory {
    fun create(folderId: String?): FolderModelFactory
}

class FolderModelFactory @AssistedInject constructor(
    private val context: Context,
    private val subscriptionsDao: SubscriptionsDao,
    private val network: INetworkSource,
    private val converters: SRSSConverters,
    @Assisted private val folderId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FolderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FolderViewModel(
                context,
                subscriptionsDao,
                network,
                converters,
                folderId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class FolderViewModel(
    private val context: Context,
    private val subscriptionsDao: SubscriptionsDao,
    private val network: INetworkSource,
    private val converters: SRSSConverters,
    folderId: String
) : ViewModel() {

    private val innerMainState: MutableStateFlow<FolderStateModel> = MutableStateFlow(FolderStateModel(folderId = folderId))
    private val innerScreenCloseFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val innerNameToEditState: MutableStateFlow<String?> = MutableStateFlow(null)
    private var innerLoadedFeedCountFlow: MutableStateFlow<Int?> = MutableStateFlow(null)

    private var innerState: FolderStateModel by Delegates.observable(innerMainState.value) { _, oldState, newState ->
        if (!oldState.isInEditMode && newState.isInEditMode) {
            innerNameToEditState.value = oldState.title
        }
        innerMainState.value = newState
    }

    private val listOfLoadingFeeds = mutableListOf<Deferred<Boolean>>()

    val state: StateFlow<FolderStateModel> = innerMainState
    val screenCloseFlow: StateFlow<Boolean> = innerScreenCloseFlow
    val nameToEditFlow: StateFlow<String?> = innerNameToEditState
    val loadedFeedCountFlow: StateFlow<Int?> = innerLoadedFeedCountFlow

    val isInEditMode: Boolean
        get() = innerState.isInEditMode

    val isInFeedListMode: Boolean
        get() = innerState.isCombinedMode

    fun updateFolderFeed(isManual: Boolean = false) {
        viewModelScope.launch {
            try {
                updateFolderFeedInternal(isManual)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun unlinkFolderByPosition(position: Int) {
        viewModelScope.launch {
            try {
                (innerState.feedList[position] as? SubscriptionItemModel)?.urlToLoad?.let { urlToLoad ->
                    subscriptionsDao.removeSingleCrossRefsByParameters(
                        urlToLoad = urlToLoad,
                        folderId = innerState.folderId
                    )
                }
                updateFolderFeed()
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

    fun checkSaveAvailability(newFolderName: String?) {
        if (!innerState.isInEditMode) return
        viewModelScope.launch {
            try {
                innerState = innerState.copy(
                    isAvailableToSave = newFolderName?.isNotEmpty() == true && innerState.title != newFolderName
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun updateFolder(newFolderName: String?) {
        viewModelScope.launch {
            try {
                newFolderName?.let { newName ->
                    subscriptionsDao.updateFolderNameById(innerState.folderId, newName)
                }
                innerState = innerState.copy(
                    isInEditMode = false
                )
                updateFolderFeedInternal(isManual = true)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun changeMode() {
        viewModelScope.launch {
            try {
                if (innerState.isCombinedMode) {
                    listOfLoadingFeeds.forEach {
                        if (it.isActive) {
                            it.cancel()
                        }
                    }
                }
                val folderWithSubscriptions =
                    subscriptionsDao.loadFolderWithSubscriptionsById(innerState.folderId)
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
        viewModelScope.launch {
            try {
                subscriptionsDao.complexRemoveFolderById(folderId = innerState.folderId)
                innerScreenCloseFlow.value = true
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    private suspend fun updateFolderFeedInternal(isManual: Boolean) {
        val folderWithSubscriptions =
            subscriptionsDao.loadFolderWithSubscriptionsById(innerState.folderId)
        if (folderWithSubscriptions?.folder?.isInFeedMode == true) {
            innerState = innerState.copy(
                isInFeedLoadingProgress = true,
                title = folderWithSubscriptions.folder.name,
                feedList = if (isManual) {
                    innerState.feedList
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
                        innerState.feedList
                    }
                },
                iterateFeedReceiver = { combinedFeedList ->
                    if (isManual) {
                        atomicUpdateFeedsList.renew(combinedFeedList)
                    } else {
                        innerState = innerState.copy(
                            feedList = combinedFeedList
                        )
                    }
                },
                exceptionHandler = { exception ->
                    LogUtils.e(TAG, exception.message, exception)
                })
            if (isManual) {
                listOfLoadingFeeds.awaitAll()
                innerState = innerState.copy(
                    feedList = atomicUpdateFeedsList
                )
            }
            val loadedFeeds = listOfLoadingFeeds.awaitAll().count { it }
            val failedToLoadListCount = listOfLoadingFeeds.size - loadedFeeds
            if (failedToLoadListCount > 0) {
                innerLoadedFeedCountFlow.value = failedToLoadListCount
            }
            listOfLoadingFeeds.clear()
            innerState = innerState.copy(
                isInFeedLoadingProgress = false
            )
        } else {
            innerState = innerState.copy(
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
            val subFeed = viewModelScope.async {
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