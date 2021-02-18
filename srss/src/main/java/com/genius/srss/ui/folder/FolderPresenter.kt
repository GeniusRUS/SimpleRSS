package com.genius.srss.ui.folder

import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.ui.subscriptions.SubscriptionItemModel
import com.ub.utils.LogUtils
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import javax.inject.Inject
import kotlin.properties.Delegates

class FolderPresenter @Inject constructor(
    private val subscriptionsDao: SubscriptionsDao
): MvpPresenter<FolderView>() {

    private var state: FolderStateModel by Delegates.observable(FolderStateModel()) { _, _, newState ->
        viewState.onStateChanged(newState)
    }

    fun updateFolderFeed(folderId: String) {
        presenterScope.launch {
            try {
                val folderWithSubscriptions = subscriptionsDao.loadFolderWithSubscriptionsById(folderId)
                state = state.copy(
                    folderId = folderId,
                    title = folderWithSubscriptions?.folder?.name,
                    feedList = folderWithSubscriptions?.subscriptions?.map {
                        SubscriptionItemModel(
                            it.title,
                            it.urlToLoad
                        )
                    } ?: emptyList()
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    fun unlinkFolderByPosition(position: Int) {
        presenterScope.launch {
            try {
                (state.feedList[position] as? SubscriptionItemModel)?.let { subscription ->
                    subscriptionsDao.removeCrossRefsById(
                        urlToLoad = subscription.urlToLoad
                    )
                }
                state.folderId?.let { folderId ->
                    updateFolderFeed(folderId)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "FolderPresenter"
    }
}