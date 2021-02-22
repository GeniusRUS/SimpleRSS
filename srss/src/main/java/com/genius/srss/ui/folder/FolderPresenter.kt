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

    private var state: FolderStateModel by Delegates.observable(FolderStateModel(folderId = folderId)) { _, _, newState ->
        viewState.onStateChanged(newState)
    }

    fun updateFolderFeed() {
        presenterScope.launch {
            try {
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
                updateFolderFeed()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "FolderPresenter"
    }
}