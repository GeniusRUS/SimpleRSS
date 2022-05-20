package com.genius.srss.ui.folder

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
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
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
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

@SuppressLint("StaticFieldLeak")
class FolderViewModel(
    private val context: Context,
    private val subscriptionsDao: SubscriptionsDao,
    private val network: INetworkSource,
    private val converters: SRSSConverters,
    folderId: String
) : ViewModel() {

    private val _state: MutableStateFlow<FolderStateModel> = MutableStateFlow(FolderStateModel(folderId = folderId))
    private val _screenCloseFlow: MutableSharedFlow<Unit> = MutableSharedFlow()
    private val _nameToEditFlow: MutableStateFlow<String?> = MutableStateFlow(null)
    private var _loadedFeedCountFlow: MutableSharedFlow<String> = MutableSharedFlow()

    private val listOfLoadingFeeds = mutableListOf<Deferred<Boolean>>()

    val state: StateFlow<FolderStateModel> = _state.distinctUntilChanged { oldState, newState ->
        if (!oldState.isInEditMode && newState.isInEditMode) {
            _nameToEditFlow.tryEmit(oldState.title)
        }
        false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _state.value
    )
    val screenCloseFlow: SharedFlow<Unit> = _screenCloseFlow
    val nameToEditFlow: StateFlow<String?> = _nameToEditFlow
    val loadedFeedCountFlow: SharedFlow<String> = _loadedFeedCountFlow

    val isInEditMode: Boolean
        get() = _state.value.isInEditMode

    val isInFeedListMode: Boolean
        get() = _state.value.isCombinedMode

    init {
        updateFolderFeed()
    }

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
                (_state.value.feedList[position] as? SubscriptionItemModel)?.urlToLoad?.let { urlToLoad ->
                    subscriptionsDao.removeSingleCrossRefsByParameters(
                        urlToLoad = urlToLoad,
                        folderId = _state.value.folderId
                    )
                }
                updateFolderFeed()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun changeEditMode(isEdit: Boolean) {
        _state.update { state ->
            state.copy(
                isInEditMode = isEdit
            )
        }
    }

    fun checkSaveAvailability(newFolderName: Editable?) {
        if (!_state.value.isInEditMode) return
        viewModelScope.launch {
            try {
                _state.update { state ->
                    state.copy(
                        isAvailableToSave = newFolderName?.isNotEmpty() == true && state.title != newFolderName.toString()
                    )
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun updateFolder(newFolderName: String?) {
        viewModelScope.launch {
            try {
                newFolderName?.let { newName ->
                    subscriptionsDao.updateFolderNameById(_state.value.folderId, newName)
                }
                _state.update { state ->
                    state.copy(
                        isInEditMode = false
                    )
                }
                updateFolderFeedInternal(isManual = true)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun changeMode() {
        viewModelScope.launch {
            try {
                if (_state.value.isCombinedMode) {
                    listOfLoadingFeeds.forEach {
                        if (it.isActive) {
                            it.cancel()
                        }
                    }
                }
                val folderWithSubscriptions =
                    subscriptionsDao.loadFolderWithSubscriptionsById(_state.value.folderId)
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
                subscriptionsDao.complexRemoveFolderById(folderId = _state.value.folderId)
                _screenCloseFlow.emit(Unit)
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    private suspend fun updateFolderFeedInternal(isManual: Boolean) {
        val folderWithSubscriptions =
            subscriptionsDao.loadFolderWithSubscriptionsById(_state.value.folderId)
        if (folderWithSubscriptions?.folder?.isInFeedMode == true) {
            _state.update { state ->
                state.copy(
                    isInFeedLoadingProgress = true,
                    title = folderWithSubscriptions.folder.name,
                    feedList = if (isManual) {
                        state.feedList
                    } else {
                        emptyList()
                    },
                    isCombinedMode = folderWithSubscriptions.folder.isInFeedMode
                )
            }
            val atomicUpdateFeedsList = mutableListOf<BaseSubscriptionModel>()
            parallelLoadingOfFeeds(
                subscriptions = folderWithSubscriptions.subscriptions,
                displayedFeedSource = {
                    if (isManual) {
                        atomicUpdateFeedsList
                    } else {
                        _state.value.feedList
                    }
                },
                iterateFeedReceiver = { combinedFeedList ->
                    if (isManual) {
                        atomicUpdateFeedsList.renew(combinedFeedList)
                    } else {
                        _state.update { state ->
                            state.copy(
                                feedList = combinedFeedList
                            )
                        }
                    }
                },
                exceptionHandler = { exception ->
                    LogUtils.e(TAG, exception.message, exception)
                })
            if (isManual) {
                listOfLoadingFeeds.awaitAll()
                _state.update { state ->
                    state.copy(
                        feedList = atomicUpdateFeedsList
                    )
                }
            }
            val loadedFeeds = listOfLoadingFeeds.awaitAll().count { it }
            val failedToLoadListCount = listOfLoadingFeeds.size - loadedFeeds
            if (failedToLoadListCount > 0) {
                _loadedFeedCountFlow.emit(
                    context.resources.getQuantityString(
                        R.plurals.folder_feed_list_not_fully_loaded,
                        failedToLoadListCount,
                        failedToLoadListCount
                    )
                )
            }
            listOfLoadingFeeds.clear()
            _state.update { state ->
                state.copy(
                    isInFeedLoadingProgress = false
                )
            }
        } else {
            _state.update { state ->
                state.copy(
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
                                actionText = context.getString(R.string.subscription_folder_add_subscription)
                            )
                        )
                    },
                    isCombinedMode = folderWithSubscriptions?.folder?.isInFeedMode ?: false
                )
            }
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
                                item.timestamp?.date?.time
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