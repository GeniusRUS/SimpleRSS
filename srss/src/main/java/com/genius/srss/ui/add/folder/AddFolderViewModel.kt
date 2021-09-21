package com.genius.srss.ui.add.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.genius.srss.R
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel
import com.genius.srss.di.services.generator.IGenerator
import com.ub.utils.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddFolderModelFactory @Inject constructor(
    private val subscriptionsDao: SubscriptionsDao,
    private val generator: IGenerator
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddFolderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddFolderViewModel(
                subscriptionsDao,
                generator
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AddFolderViewModel(
    private val subscriptionsDao: SubscriptionsDao,
    private val generator: IGenerator
) : ViewModel() {

    private val innerErrorFlow: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val innerFolderCreatedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val errorFlow: StateFlow<Int?> = innerErrorFlow
    val folderCreatedFlow: StateFlow<Boolean> = innerFolderCreatedFlow

    fun saveFolder(folderName: String) {
        viewModelScope.launch {
            try {
                if (folderName.isEmpty()) {
                    innerErrorFlow.value = R.string.add_new_subscription_folder_must_be_not_empty
                    return@launch
                }
                subscriptionsDao.saveFolder(
                    SubscriptionFolderDatabaseModel(
                        generator.generateRandomId(),
                        folderName,
                        System.currentTimeMillis()
                    )
                )
                innerFolderCreatedFlow.value = true
            } catch (e: Exception) {
                LogUtils.e(TAG, e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "AddFolderPresenter"
    }
}