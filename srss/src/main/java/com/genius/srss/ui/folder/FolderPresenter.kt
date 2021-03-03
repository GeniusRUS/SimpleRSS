package com.genius.srss.ui.folder

import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.ui.subscriptions.SubscriptionItemModel
import com.ub.utils.LogUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import kotlin.properties.Delegates

@AssistedFactory
interface FolderPresenterFactory {
    fun create(folderId: String?): FolderPresenter
}

class FolderPresenter @AssistedInject constructor(
    private val subscriptionsDao: SubscriptionsDao,
    @Assisted folderId: String
): MvpPresenter<FolderView>() {

    private var state: FolderStateModel by Delegates.observable(FolderStateModel(folderId = folderId)) { _, oldState, newState ->
        if (!oldState.isInEditMode && newState.isInEditMode) {
            viewState.onUpdateNameToEdit(oldState.title)
        }
        viewState.onStateChanged(newState)
    }

    val isInEditMode: Boolean
        get() = state.isInEditMode

    fun updateFolderFeed() {
        presenterScope.launch {
            try {
                updateFolderFeedInternal()
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
                updateFolderFeedInternal()
                state = state.copy(
                    isInEditMode = false
                )
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

    private suspend fun updateFolderFeedInternal() {
        val folderWithSubscriptions = subscriptionsDao.loadFolderWithSubscriptionsById(state.folderId)
        state = state.copy(
            title = folderWithSubscriptions?.folder?.name,
            feedList = folderWithSubscriptions?.subscriptions?.map {
                SubscriptionItemModel(
                    it.title,
                    it.urlToLoad
                )
            } ?: emptyList()
        )
    }

    companion object {
        private const val TAG = "FolderPresenter"
    }
}