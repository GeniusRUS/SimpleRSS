package com.genius.srss.ui.add.folder

import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel
import com.genius.srss.di.services.generator.IGenerator
import com.ub.utils.LogUtils
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import javax.inject.Inject

class AddFolderPresenter @Inject constructor(
    private val subscriptionsDao: SubscriptionsDao,
    private val generator: IGenerator
) : MvpPresenter<AddFolderView>() {

    fun saveFolder(folderName: String) {
        presenterScope.launch {
            try {
                if (folderName.isEmpty()) {
                    viewState.showErrorMessage(R.string.add_new_subscription_folder_must_be_not_empty)
                    return@launch
                }
                subscriptionsDao.saveFolder(
                    SubscriptionFolderDatabaseModel(
                        generator.generateRandomId(),
                        folderName,
                        System.currentTimeMillis()
                    )
                )
                viewState.onFolderCreated()
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "AddFolderPresenter"
    }
}